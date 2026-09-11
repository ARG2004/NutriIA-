/**
 * Script de Migración Online de Fechas a Timestamp para Firestore (NutrIA)
 * Usa las credenciales activas de Firebase CLI para conectarse a Cloud Firestore
 * y actualizar documentos existentes que tienen fechas en String a Timestamp real.
 */

const fs = require('fs');
const path = require('path');
const os = require('os');
const https = require('https');

const PROJECT_ID = 'nutria-878de';

// 1. Obtener Token de Acceso desde Firebase CLI
function getTokens() {
  const configPath = path.join(os.homedir(), '.config', 'configstore', 'firebase-tools.json');
  if (!fs.existsSync(configPath)) {
    throw new Error('No se encontró sesión activa de Firebase CLI.');
  }
  const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  const tokens = config.tokens || {};
  return tokens;
}

async function refreshAccessToken(refreshToken) {
  return new Promise((resolve, reject) => {
    const data = new URLSearchParams({
      client_id: '563584335869-fgrhgmd47bqnekij5i8b5pr03ho85qd6.apps.googleusercontent.com',
      client_secret: '', // Public client
      grant_type: 'refresh_token',
      refresh_token: refreshToken
    }).toString();

    const req = https.request('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    }, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(body);
          if (parsed.access_token) resolve(parsed.access_token);
          else reject(new Error('No se pudo refrescar el token: ' + body));
        } catch (e) {
          reject(e);
        }
      });
    });
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

async function getValidAccessToken() {
  const tokens = getTokens();
  if (tokens.access_token && tokens.expires_at && tokens.expires_at > Date.now() + 60000) {
    return tokens.access_token;
  }
  if (tokens.refresh_token) {
    return await refreshAccessToken(tokens.refresh_token);
  }
  return tokens.access_token;
}

// 2. Cliente REST para Firestore
async function firestoreRequest(token, path, method = 'GET', data = null) {
  return new Promise((resolve, reject) => {
    const url = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents${path}`;
    const urlObj = new URL(url);
    const options = {
      hostname: urlObj.hostname,
      path: urlObj.pathname + urlObj.search,
      method: method,
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    };

    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        try {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(body ? JSON.parse(body) : {});
          } else {
            resolve({ error: { code: res.statusCode, message: body } });
          }
        } catch (e) {
          reject(e);
        }
      });
    });
    req.on('error', reject);
    if (data) req.write(JSON.stringify(data));
    req.end();
  });
}

// 3. Conversión de Formatos de Fecha a ISO String (Timestamp)
function parseStringToIso(str) {
  if (!str || typeof str !== 'string') return null;
  const clean = str.trim();

  // 1. Epoch Millis como String
  const asNum = Number(clean);
  if (!isNaN(asNum) && asNum > 1000000000) {
    const millis = asNum > 100000000000 ? asNum : asNum * 1000;
    return new Date(millis).toISOString();
  }

  // 2. ISO 8601 (2026-09-11T...)
  const isoDate = new Date(clean);
  if (!isNaN(isoDate.getTime()) && (clean.includes('-') || clean.includes('T'))) {
    return isoDate.toISOString();
  }

  // 3. Formato dd/MM/yyyy HH:mm:ss o dd/MM/yyyy
  if (clean.includes('/')) {
    const parts = clean.split(' ');
    const dateParts = parts[0].split('/');
    if (dateParts.length === 3) {
      const day = parseInt(dateParts[0], 10);
      const month = parseInt(dateParts[1], 10) - 1;
      const year = parseInt(dateParts[2], 10);

      let hour = 12, min = 0, sec = 0;
      if (parts[1]) {
        const timeParts = parts[1].split(':');
        hour = parseInt(timeParts[0] || '0', 10);
        min = parseInt(timeParts[1] || '0', 10);
        sec = parseInt(timeParts[2] || '0', 10);
      }
      const d = new Date(Date.UTC(year, month, day, hour, min, sec));
      if (!isNaN(d.getTime())) {
        return d.toISOString();
      }
    }
  }

  return null;
}

const DATE_FIELDS = new Set([
  'creadoEn',
  'actualizadoEn',
  'createdAt',
  'updatedAt',
  'fechaConsentimiento'
]);

async function migrarDoc(token, doc) {
  if (!doc.fields) return 0;
  const docName = doc.name; // projects/nutria-878de/databases/(default)/documents/...
  const relPath = docName.replace(`projects/${PROJECT_ID}/databases/(default)/documents`, '');

  const updateMaskFields = [];
  const updatedFields = {};
  let changed = false;

  for (const [key, val] of Object.entries(doc.fields)) {
    if (DATE_FIELDS.has(key)) {
      if (val.stringValue) {
        const iso = parseStringToIso(val.stringValue);
        if (iso) {
          updateMaskFields.push(key);
          updatedFields[key] = { timestampValue: iso };
          changed = true;
          console.log(`  [MIGRADO] ${relPath} -> ${key}: "${val.stringValue}" => Timestamp(${iso})`);
        }
      } else if (val.integerValue) {
        const num = Number(val.integerValue);
        if (num > 1000000000) {
          const millis = num > 100000000000 ? num : num * 1000;
          const iso = new Date(millis).toISOString();
          updateMaskFields.push(key);
          updatedFields[key] = { timestampValue: iso };
          changed = true;
          console.log(`  [MIGRADO] ${relPath} -> ${key}: ${num} => Timestamp(${iso})`);
        }
      }
    }
  }

  if (changed) {
    const maskQuery = updateMaskFields.map(f => `updateMask.fieldPaths=${f}`).join('&');
    const patchPath = `${relPath}?${maskQuery}`;
    const res = await firestoreRequest(token, patchPath, 'PATCH', { fields: updatedFields });
    if (res.error) {
      console.error(`  [ERROR AL GUARDAR] ${relPath}:`, res.error.message);
      return 0;
    }
    return 1;
  }
  return 0;
}

async function listarYMigrarColeccion(token, colPath) {
  const cleanPath = colPath.startsWith('/') ? colPath : `/${colPath}`;
  console.log(`\n🔍 Escaneando ${cleanPath}...`);
  let pageToken = '';
  let total = 0;

  do {
    const urlPath = `${cleanPath}${pageToken ? `?pageToken=${pageToken}` : ''}`;
    const res = await firestoreRequest(token, urlPath);
    if (res.error) {
      break;
    }
    const docs = res.documents || [];
    for (const doc of docs) {
      total += await migrarDoc(token, doc);
    }
    pageToken = res.nextPageToken || '';
  } while (pageToken);

  return total;
}

async function main() {
  console.log('===============================================================');
  console.log('🚀 Conectando a Cloud Firestore (nutria-878de)...');
  console.log('===============================================================');

  const token = await getValidAccessToken();
  console.log('✅ Autenticado exitosamente con credenciales de Firebase CLI.');

  let totalMigrados = 0;

  // 1. Colecciones principales
  const colecciones = [
    'usuarios',
    'vinculaciones',
    'vinculaciones_embarazo',
    'nutriologos_publicos',
    'ginecologos_publicos',
    'teleconsultas',
    'pagos_teleconsulta'
  ];

  for (const col of colecciones) {
    totalMigrados += await listarYMigrarColeccion(token, col);
  }

  // 2. Subcolecciones dentro de cada usuario
  console.log('\n🔍 Escaneando subcolecciones de usuarios (hijos, consultas, recetas, etc.)...');
  const resUsuarios = await firestoreRequest(token, '/usuarios');
  const usuarios = resUsuarios.documents || [];

  for (const userDoc of usuarios) {
    const userRelPath = userDoc.name.replace(`projects/${PROJECT_ID}/databases/(default)/documents`, '');

    // Subcolecciones directas del usuario
    for (const sub of ['perfilEmbarazo', 'nutrientes', 'alertas']) {
      totalMigrados += await listarYMigrarColeccion(token, `${userRelPath}/${sub}`);
    }

    // Hijos del usuario
    const resHijos = await firestoreRequest(token, `${userRelPath}/hijos`);
    const hijos = resHijos.documents || [];
    for (const hijoDoc of hijos) {
      totalMigrados += await migrarDoc(token, hijoDoc);
      const hijoRelPath = hijoDoc.name.replace(`projects/${PROJECT_ID}/databases/(default)/documents`, '');

      const subHijos = [
        'consultas',
        'recetas_nutriologo',
        'planes_alimentarios',
        'lactancia_tomas',
        'extraccion_manual',
        'solidos',
        'crecimiento'
      ];
      for (const sub of subHijos) {
        totalMigrados += await listarYMigrarColeccion(token, `${hijoRelPath}/${sub}`);
      }
    }
  }

  console.log('\n===============================================================');
  console.log(`🎉 Migración completada. Documentos actualizados a Timestamp: ${totalMigrados}`);
  console.log('===============================================================');
}

main().catch(err => {
  console.error('Error fatal:', err);
  process.exit(1);
});
