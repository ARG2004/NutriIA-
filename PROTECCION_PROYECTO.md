# Manual de Seguridad, Protección y Propiedad Intelectual - Proyecto NutriIA

Este documento establece las políticas de seguridad técnica, arquitectura de protección de datos, directrices de desarrollo y lineamientos de propiedad intelectual para la protección integral del proyecto **NutriIA** (código fuente, modelos de inteligencia artificial, bases de datos y activos digitales).

---

## 1. Declaración de Propiedad Intelectual y Confidencialidad

### 1.1 Titularidad del Código y Activos
Todos los componentes del software NutriIA, que incluyen de manera enunciativa más no limitativa:
* El código fuente escrito en Kotlin, Swift y scripts de compilación.
* Los algoritmos de clasificación, incluyendo el **Clasificador Anatómico Relacional 3D LSM** (`SignLanguageClassifier`).
* Las estructuras y repositorios de análisis de Inteligencia Artificial (`AnalisisRepository`, `PlanEmbarazoIARepository`, `ChatbotRepository`).
* Las bases de datos locales, esquemas de bases de datos remotas (Firestore) y archivos de calibración/dataset (`lsm_dataset.json`).
* Los recursos gráficos, logotipos, interfaces de usuario y documentación técnica asociados.

Son propiedad intelectual exclusiva del titular del proyecto. Queda estrictamente prohibida su copia, distribución, reproducción, modificación, descompilación o comercialización sin la autorización expresa y por escrito del propietario.

### 1.2 Acuerdo de Confidencialidad (NDA)
Cualquier desarrollador, colaborador o tercero con acceso directo al código fuente o recursos de NutriIA asume las siguientes obligaciones:
1. **Confidencialidad:** Mantener en estricta reserva toda la información técnica, lógica de negocio y arquitectura de software del proyecto.
2. **Uso Exclusivo:** Utilizar el código fuente únicamente para fines de desarrollo, depuración y mejora autorizados por el titular.
3. **Prohibición de Extracción:** No extraer partes del software o algoritmos para proyectos ajenos a NutriIA.

---

## 2. Arquitectura de Seguridad y Protección de Credenciales

Para garantizar la máxima seguridad y evitar la filtración o ingeniería inversa sobre credenciales de servicios externos:

### 2.1 Aislamiento Total de Claves en Entornos de Desarrollo
* Las claves de producción y credenciales maestras de servicios de terceros **nunca** se escriben en texto plano dentro de archivos rastreados por Git.
* Todas las variables de entorno se gestionan mediante el archivo local `local.properties` (excluido en `.gitignore`) o a través de **GitHub Actions Secrets** en el pipeline de CI/CD.

### 2.2 Arquitectura de Comunicación de Backend (Proxying)
* Para prevenir la extracción de claves API en clientes compilados (APK / IPA), la comunicación con servicios sensibles se enruta mediante peticiones autenticadas utilizando tokens de sesión temporales (OAuth 2.0 / JWT).
* Ningún token maestro permanente es distribuido en los paquetes finales del cliente.

---

## 3. Protección del Código Fuente (Minificación y Ofuscación)

Para mitigar el riesgo de ingeniería inversa y descompilación de binarios por parte de terceros:

### 3.1 Ofuscación de Binarios Android (R8 / ProGuard)
En el entorno de producción Android (`build.gradle.kts`), R8 optimiza y ofusca el mapa de clases, métodos y campos:
* **Minificación Activada:** `isMinifyEnabled = true` y `isShrinkResources = true`.
* **Reglas Específicas:** Conservación de hooks JNI nativos (MediaPipe / Protobuf) previniendo ingeniería inversa sobre la lógica de negocio.

### 3.2 Protección de Binarios iOS (Hardened Runtime)
En la compilación nativa para iOS:
* **Hardened Runtime (`--options runtime`):** Impide la inyección de código dinámico en tiempo de ejecución.
* **Eliminación de Símbolos (`Strip Debug Symbols`):** Elimina nombres de funciones y depuración en la variante *Release*.

---

## 4. Protección y Privacidad de los Datos del Usuario

Al procesar información de salud y nutrición, se aplican las siguientes medidas de privacidad:

### 4.1 Cifrado y Almacenamiento Seguro en Dispositivos
* **iOS:** Almacenamiento exclusivo de tokens en **iOS Keychain** (`kSecClassGenericPassword`).
* **Android:** Almacenamiento mediante **EncryptedSharedPreferences** respaldado por Android Keystore.

### 4.2 Reglas de Seguridad en la Nube (Firebase Firestore)
* **Principio de Privilegio Mínimo:** Las reglas del servidor Firestore restringen las operaciones de lectura y escritura exclusivamente al usuario autenticado sobre su propia colección (`/usuarios/{userId}/...`).

---

## 5. Gestión del Ciclo de Vida del Software y Firmas Digitales

### 5.1 Firma Digital (Keystore / Apple Distribution)
* Los certificados `.p12` de Apple y `.keystore` / `.jks` de Android se mantienen en almacenamiento seguro y nunca en repositorios públicos.
* El pipeline de CI/CD inyecta y firma dinámicamente los binarios durante el empaquetado de lanzamiento sin persistir credenciales en disco.

