# Manual de Seguridad, Protección y Propiedad Intelectual - Proyecto NutriIA

Este documento establece las políticas de seguridad técnica, directrices de desarrollo y lineamientos legales para la protección integral del proyecto **NutriIA** (código fuente, modelos de inteligencia artificial, bases de datos y propiedad intelectual).

---

## 1. Declaración de Propiedad Intelectual y Confidencialidad

### 1.1 Titularidad del Código y Activos
Todos los componentes del software NutriIA, que incluyen de manera enunciativa más no limitativa:
* El código fuente escrito en Kotlin y archivos de configuración Gradle.
* Los algoritmos de clasificación, incluyendo el **Clasificador Anatómico Relacional 3D LSM** (`SignLanguageClassifier`).
* Las estructuras y repositorios de análisis de Inteligencia Artificial (`AnalisisRepository`, `PlanEmbarazoIARepository`, `ChatbotRepository`).
* Las bases de datos locales (Room), los esquemas de bases de datos remotas (Firestore) y archivos de calibración/dataset (`lsm_dataset.json`).
* Los recursos gráficos, logotipos, interfaces de usuario y documentación técnica asociados.

Son propiedad intelectual exclusiva del titular del proyecto. Queda estrictamente prohibida su copia, distribución, reproducción, modificación, descompilación o comercialización sin una autorización expresa y por escrito del propietario.

### 1.2 Acuerdo de Confidencialidad para Desarrolladores (NDA)
Cualquier desarrollador, colaborador o tercero con acceso directo al código fuente o recursos de NutriIA asume las siguientes obligaciones:
1. **Confidencialidad:** Mantener en estricta reserva toda la información técnica, lógica de negocio y arquitectura de software del proyecto.
2. **Uso Exclusivo:** Utilizar el código fuente únicamente para fines de desarrollo, depuración y mejora autorizados por el titular.
3. **Prohibición de Extracción:** No extraer partes del software para proyectos ajenos a NutriIA.

---

## 2. Seguridad y Protección de Credenciales (API Keys)

Para evitar la exposición accidental de claves privadas de servicios de terceros (como Groq, HuggingFace y Spoonacular), el proyecto implementa una arquitectura de aislamiento y codificación durante el ciclo de vida del software.

### 2.1 Aislamiento de Credenciales locales
Las claves API reales **nunca** deben ser escritas directamente en el código del repositorio ni subirse a Git.
* Todas las claves se configuran localmente en el archivo `local.properties`, el cual se encuentra excluido del control de versiones en el `.gitignore`.

### 2.2 Flujo de Compilación y Codificación (Base64)
Durante el proceso de compilación, Gradle extrae las variables del archivo de propiedades locales y las codifica en Base64 para evitar que se guarden en texto plano dentro del binario.
Este flujo está automatizado en el archivo `app/build.gradle.kts`:

```kotlin
val encodeKey = { key: String ->
    val raw = localProperties[key] as? String ?: ""
    if (raw.isNotEmpty() && raw != "TU_CLAVE_GROQ_AQUI") {
        Base64.getEncoder().encodeToString(raw.toByteArray())
    } else {
        raw
    }
}
```

### 2.3 Desofuscación en Tiempo de Ejecución
Cuando la aplicación realiza peticiones seguras (por ejemplo, en `AnalisisRepository.detectarAlimento` o `ChatbotRepository`), las claves se descodifican en memoria únicamente cuando son necesarias utilizando el descodificador seguro `KeyDeobfuscator.kt`:

```kotlin
val apiKey = KeyDeobfuscator.deobfuscate(BuildConfig.GROQ_API_KEY)
```

---

## 3. Protección del Código Fuente (Minificación y Ofuscación)

Para mitigar el riesgo de ingeniería inversa y descompilación de binarios APK por parte de terceros, NutriIA utiliza R8 (el optimizador de código de Android).

### 3.1 Configuración de Compilación (Release)
La minificación y optimización están activadas por defecto para la variante de producción en `app/build.gradle.kts`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 3.2 Reglas Específicas de Ofuscación
Para garantizar que las librerías críticas de procesamiento de imágenes y modelos integrados (como MediaPipe, Gson, y las calibraciones personalizadas LSM) sigan funcionando correctamente sin romper la lógica del negocio, se mantienen reglas específicas en `proguard-rules.pro`:

* **Preservación de interfaces de deserialización:** Se conserva la estructura de los modelos LSM bajo el paquete `com.example.nutriia.accesibilidad.*`.
* **Preservación de llamadas nativas (JNI):** MediaPipe y clases de Protobuf se mantienen intactos para prevenir excepciones en tiempo de ejecución.

---

## 4. Protección y Privacidad de los Datos del Usuario

Al procesar información sensible (datos pediátricos, planes de alimentación de embarazo y análisis de nutrición de menores), se deben observar estrictamente las siguientes medidas de seguridad en el manejo de datos:

### 4.1 Cifrado y Almacenamiento Local
* **Bases de Datos locales (Room):** El acceso al almacenamiento interno de la aplicación está protegido por el sistema de sandbox de Android.
* **Seguridad de Preferencias:** Las configuraciones críticas del usuario se almacenan en `DataStore Preferences`, evitando el uso de ficheros de texto plano legibles en el almacenamiento externo.

### 4.2 Reglas de Seguridad en la Nube (Firebase Firestore)
Para la persistencia en Firebase Firestore, se deben estructurar reglas de seguridad del lado del servidor que verifiquen la identidad del usuario antes de permitir cualquier operación de lectura o escritura.
* **Principio de Privilegio Mínimo:** Un usuario autenticado únicamente debe tener permitido leer y escribir en su propia colección dentro de `/usuarios/{userId}/...`.
* **Validación de Datos:** Restringir el esquema de datos en Firestore para evitar que usuarios malintencionados inyecten registros inválidos.

---

## 5. Gestión del Ciclo de Vida del Software (CI/CD y Firma digital)

### 5.1 Almacén de Claves de Lanzamiento (Keystore)
El archivo `.jks` o `.keystore` utilizado para firmar digitalmente la aplicación de producción **nunca debe estar en el repositorio público ni privado**.
* Debe guardarse en un almacenamiento seguro o ser inyectado como variable de entorno secreta en el servidor de CI/CD (GitHub Actions, GitLab CI, etc.).
* Las contraseñas del Keystore y alias de firma deben guardarse bajo un gestor de secretos y no escribirse en archivos legibles del proyecto.

### 5.2 Rotación de Credenciales
Es una buena práctica rotar y actualizar las API keys y certificados de firma periódicamente en la consola de administración de cada proveedor para mitigar filtraciones en el ciclo de vida del desarrollo.
