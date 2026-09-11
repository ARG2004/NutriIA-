/**
 * Script de Migración de Fechas a Timestamp para Cloud Firestore (NutrIA)
 * Convierte automáticamente campos de fecha almacenados en String o Number a Timestamp oficial de Firestore.
 */

const { initializeApp, cert, applicationDefault } = require('firebase-admin/app');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');

// Inicializar Firebase Admin
try {
  initializeApp({
    projectId: 'nutria-878de'
  });
} catch (e) {
  console.log('Admin SDK ya inicializado o usando configuración por defecto');
}

const db = getFirestore();

// Campos de fecha conocidos en el esquema de datos
const DATE_FIELD_NAMES = new Set([
  'creadoEn',
  'actualizadoEn',
  'createdAt',
  'updatedAt',
  'fechaConsentimiento',
  'fechaCreacion',
  'fechaHora'
]);

function parseStringToTimestamp(str) {
  if (!str || typeof str !== 'string') return null;
  const clean = str.trim();

  // 1. Epoch Millis como String
  const asNum = Number(clean);
  if (!isNaN(asNum) && asNum > 1000000000) {
    const millis = asNum > 100000000000 ? asNum : asNum * 1000;
    return Timestamp.fromMillis(millis);
  }

  // 2. ISO 8601 (2026-09-11T...)
  const isoDate = new Date(clean);
  if (!isNaN(isoDate.getTime())) {
    return Timestamp.fromDate(isoDate);
  }

  // 3. Formato dd/MM/yyyy HH:mm:ss o dd/MM/yyyy
  if (clean.includes('/')) {
    const parts = clean.split(' ');
    const dateParts = parts[0].split('/');
    if (dateParts.length === 3) {
      const day = parseInt(dateParts[0], 10);
      const month = parseInt(dateParts[1], 10) - 1;
      const year = parseInt(dateParts[2], 10);

      let hour = 0, min = 0, sec = 0;
      if (parts[1]) {
        const timeParts = parts[1].split(':');
        hour = parseInt(timeParts[0] || '0', 10);
        min = parseInt(timeParts[1] || '0', 10);
        sec = parseInt(timeParts[2] || '0', 10);
      }
      const d = new Date(year, month, day, hour, min, sec);
      if (!isNaN(d.getTime())) {
        return Timestamp.fromDate(d);
      }
    }
  }

  return null;
}

async function migrarDocumento(docRef, data, dryRun = false) {
  const updates = {};
  let modified = false;

  for (const [key, value] of Object.entries(data)) {
    if (DATE_FIELD_NAMES.has(key)) {
      if (typeof value === 'string') {
        const ts = parseStringToTimestamp(value);
        if (ts) {
          updates[key] = ts;
          modified = true;
          console.log(`  [MIGRADO] ${docRef.path} -> ${key}: "${value}" => Timestamp(${ts.toDate().toISOString()})`);
        }
      } else if (typeof value === 'number' && value > 1000000000) {
        const millis = value > 100000000000 ? value : value * 1000;
        const ts = Timestamp.fromMillis(millis);
        updates[key] = ts;
        modified = true;
        console.log(`  [MIGRADO] ${docRef.path} -> ${key}: ${value} => Timestamp(${ts.toDate().toISOString()})`);
      }
    }
  }

  if (modified && !dryRun) {
    await docRef.update(updates);
  }
  return modified ? 1 : 0;
}

async function migrarColeccion(collectionPath, dryRun = false) {
  console.log(`\n🔍 Escaneando colección: ${collectionPath}...`);
  let count = 0;
  try {
    const snapshot = await db.collection(collectionPath).get();
    for (const doc of snapshot.docs) {
      const mig = await migrarDocumento(doc.ref, doc.data(), dryRun);
      count += mig;
    }
  } catch (err) {
    console.error(`Error en ${collectionPath}:`, err.message);
  }
  return count;
}

async function migrarTodo(dryRun = false) {
  console.log(`=======================================================`);
  console.log(`🚀 Iniciando Migración de Fechas a Timestamp (dryRun: ${dryRun})`);
  console.log(`=======================================================`);

  let totalMigrados = 0;

  // 1. Colecciones Raíz
  const coleccionesRaiz = [
    'usuarios',
    'vinculaciones',
    'vinculaciones_embarazo',
    'nutriologos_publicos',
    'ginecologos_publicos',
    'teleconsultas',
    'pagos_teleconsulta'
  ];

  for (const col of coleccionesRaiz) {
    totalMigrados += await migrarColeccion(col, dryRun);
  }

  // 2. Subcolecciones de usuarios (hijos, consultas, recetas, etc.)
  console.log(`\n🔍 Escaneando subcolecciones de usuarios...`);
  try {
    const usuariosSnap = await db.collection('usuarios').get();
    for (const userDoc of usuariosSnap.docs) {
      // Hijos
      const hijosSnap = await userDoc.ref.collection('hijos').get();
      for (const hijoDoc of hijosSnap.docs) {
        totalMigrados += await migrarDocumento(hijoDoc.ref, hijoDoc.data(), dryRun);

        // Subcolecciones dentro de hijos
        const subcols = [
          'consultas',
          'recetas_nutriologo',
          'planes_alimentarios',
          'lactancia_tomas',
          'extraccion_manual',
          'solidos',
          'crecimiento'
        ];

        for (const sub of subcols) {
          const subSnap = await hijoDoc.ref.collection(sub).get();
          for (const subDoc of subSnap.docs) {
            totalMigrados += await migrarDocumento(subDoc.ref, subDoc.data(), dryRun);
          }
        }
      }

      // perfilEmbarazo, nutrientes, alertas
      for (const sub of ['perfilEmbarazo', 'nutrientes', 'alertas']) {
        const subSnap = await userDoc.ref.collection(sub).get();
        for (const subDoc of subSnap.docs) {
          totalMigrados += await migrarDocumento(subDoc.ref, subDoc.data(), dryRun);
        }
      }
    }
  } catch (err) {
    console.error(`Error en subcolecciones:`, err.message);
  }

  console.log(`\n=======================================================`);
  console.log(`✅ Migración Finalizada. Total de documentos actualizados: ${totalMigrados}`);
  console.log(`=======================================================`);
}

// Ejecutar
const isDryRun = process.argv.includes('--dry-run');
migrarTodo(isDryRun).then(() => {
  process.exit(0);
}).catch(err => {
  console.error('Error fatal en migración:', err);
  process.exit(1);
});
