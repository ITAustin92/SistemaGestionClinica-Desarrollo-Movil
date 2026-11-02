# CMQ Salud — Gestión documental

Aplicación Android para registrar, consultar y revisar documentos laborales y administrativos de funcionarios de CMQ Salud. Desarrollada con **Kotlin, Jetpack Compose, arquitectura MVVM y SQLite** como proyecto académico de DSY1105.

| Información académica | Detalle |
| --- | --- |
| Integrantes | Ángel Quintero, Food Fontus y Máximo Lugo |
| Sección | 004-D |
| Docente | Bastian Lisperguer |
| Versión | 0.3.0 — Acceso biométrico del revisor |

## Objetivo y estado actual

El proyecto busca facilitar la organización y el seguimiento de antecedentes documentales mediante dos perfiles: funcionario y revisor. Incluye seis pantallas conectadas: acceso, listado, registro, detalle, pendientes y perfil.

Los usuarios y los documentos iniciales son ficticios. El acceso permite seleccionar perfiles de demostración sin credenciales institucionales. El botón «Verificar identidad y entrar» utiliza la autenticación biométrica del dispositivo. Los documentos añadidos, los adjuntos y las revisiones **permanecen al cerrar y volver a abrir la aplicación**. Al iniciar un nuevo proceso se vuelve a seleccionar el perfil; los datos se recuperan desde SQLite.

## Funcionalidades disponibles

- Listado de documentos con búsqueda por título y filtros por tipo y estado.
- Registro con título, tipo, observación y un adjunto.
- Digitalización de una página mediante la cámara del dispositivo, guardada como imagen JPG.
- Vista previa de imágenes dentro de la aplicación, respetando la orientación de la cámara.
- Selección de archivos PDF, JPG y PNG mediante el selector del sistema, con límite de 10 MiB.
- Copia privada de los adjuntos seleccionados: el registro no depende de conservar el archivo original.
- Visualización de PDF dentro de la aplicación, con navegación por páginas y apertura externa opcional.
- Validación del formulario y mensajes de error o confirmación.
- Consulta del detalle, estado e historial de un documento.
- Aprobación o rechazo por el revisor; el rechazo requiere una observación.
- Bandejas **Pendientes**, **Aprobados** y **Rechazados** para consultar las decisiones propias y abrir el detalle con sus observaciones.
- Acceso del revisor protegido con el diálogo biométrico de Android.
- Menú del avatar con acceso a **Mi perfil** y cierre de sesión.
- Reinicio de la demostración con confirmación desde la pantalla de perfil.

El funcionario consulta sus propios documentos y el revisor consulta los de otros titulares de su centro. Estas restricciones corresponden al prototipo académico; no constituyen autenticación institucional.

Los cinco documentos iniciales contienen PDF de muestra con el título, titular y centro correspondientes. La opción **Generar PDF de ejemplo al guardar** también crea un adjunto real con los datos del formulario. Estos archivos tienen formato de certificado, antecedente laboral o registro administrativo y están identificados como documentos de demostración.

Los perfiles son **Ángel Quintero**, **Food Fontus**, **Máximo Lugo** y **Dr. Pedro Soto** (revisor). Ángel, Food y el revisor pertenecen al **Centro Médico Quilpué**; Máximo pertenece al **Centro Médico Belloto**. Al actualizar desde la versión anterior se cambian las etiquetas antiguas y se completan los adjuntos de muestra sin borrar los registros, las revisiones ni los archivos ya guardados.

## Arquitectura MVVM

```text
Interfaz Compose (ui)
       ↓ eventos
CmqViewModel (viewmodel)
       ↓ operaciones en Dispatchers.IO
DocumentoRepository
       └── SQLiteDocumentoRepository
              ├── Reglas de registro y revisión
              ├── DocumentoDatabase → SQLite
              └── ArchivoAdjuntoStore → archivos privados
       ↓ StateFlow
Nuevo estado → interfaz actualizada
```

| Capa | Responsabilidad |
| --- | --- |
| `ui/` | Presentar el estado, recibir acciones y gestionar navegación, contratos de cámara/selector y vista previa. |
| `viewmodel/` | Gestionar perfil activo, filtros, formulario, validaciones, carga, errores y eventos. Coordinar las operaciones de disco fuera del hilo de la interfaz. |
| `model/` | Definir usuarios, documentos, adjuntos, revisiones, eventos y estados. |
| `repository/` | Aplicar restricciones, acceder a SQLite y gestionar los archivos temporales y definitivos. |

`MainActivity`, basada en `FragmentActivity`, inicia Compose y conecta AndroidX Biometric con el ViewModel. `CmqApplication` proporciona las instancias compartidas del repositorio y del almacén de adjuntos. `CmqApp` contiene el `NavHost`, los contratos de Activity Result y los callbacks de navegación. El ViewModel no conoce el `NavController`.

El repositorio SQLite reutiliza las reglas de `DemoDocumentoRepository`, cuya implementación en memoria también permite probarlas por separado. Cada operación guarda primero los cambios en una transacción y publica después el nuevo estado. La implementación actual reescribe el conjunto de documentos dentro de la transacción, una solución acotada para el volumen del prototipo.

### Estructura del código

```text
app/src/main/java/cl/cmq/salud/
├── MainActivity.kt
├── CmqApplication.kt
├── ui/
│   ├── CmqApp.kt
│   ├── access/
│   ├── documents/
│   ├── documentform/
│   ├── documentdetail/
│   ├── review/
│   ├── profile/
│   ├── components/          # Componentes reutilizables y visores de imágenes/PDF
│   └── theme/
├── viewmodel/
├── model/
└── repository/
    ├── DocumentoRepository.kt
    ├── DemoDocumentoRepository.kt
    ├── SQLiteDocumentoRepository.kt
    ├── ArchivoAdjuntoStore.kt
    └── local/
        ├── DocumentoDatabase.kt
        ├── DocumentoEjemploPdf.kt
        └── HelveticaMetricas.kt
```

## Persistencia y archivos

SQLite se utiliza directamente mediante `SQLiteOpenHelper`, sin Room ni un servidor externo. La base privada `cmq_documentos.db` contiene:

| Tabla | Contenido |
| --- | --- |
| `documentos` | Metadatos, titular, centro, estado, datos del adjunto y última revisión. |
| `eventos` | Historial ordenado; clave foránea al documento con borrado en cascada. |
| `configuracion` | Indicador de inicialización para insertar los ejemplos solo una vez. |

Las imágenes y los PDF se guardan en `files/adjuntos/`, dentro del almacenamiento privado. SQLite guarda su ruta y metadatos, no el contenido binario. Las capturas e importaciones todavía no guardadas utilizan `cache/adjuntos_pendientes/`; los borradores no son persistentes y pueden perderse al terminar el proceso.

La cámara se abre con `ActivityResultContracts.TakePicture`. Un `FileProvider` concede acceso temporal al archivo de captura. La app delega la captura en la aplicación de cámara del dispositivo y no solicita acceso general al almacenamiento ni permiso directo de cámara. La importación usa `OpenDocument` y copia el contenido seleccionado al espacio privado.

## Requisitos

- Android Studio compatible con Android Gradle Plugin 9.1.1.
- JDK 25 para el daemon de Gradle, según `gradle/gradle-daemon-jvm.properties`.
- Android SDK Platform **37.0** y Build Tools **36.0.0**.
- Emulador o dispositivo físico con **Android 8.0 / API 26 o superior**.
- Aplicación de cámara disponible para capturar imágenes; para probar en emulador, una cámara virtual configurada.
- Sensor biométrico compatible y una huella o biometría fuerte registrada para utilizar la verificación biométrica del revisor. Los funcionarios no requieren biometría.
- Conexión a Internet durante la primera sincronización y primera descarga de las dependencias de prueba.

El proyecto incluye Gradle Wrapper. El bytecode de la aplicación se configura para Java/Kotlin 17, independientemente del JDK del daemon.

## Ejecución en Android Studio

1. Clonar el repositorio y seleccionar la rama que contiene esta versión. La rama de desarrollo inicial es `dev-angel`.
2. Abrir la carpeta raíz que contiene `settings.gradle.kts` y `gradlew`, en lugar de abrir solamente `app/`.
3. Instalar los componentes indicados desde **SDK Manager** y esperar a que termine la sincronización de Gradle.
4. Revisar la configuración de Gradle JDK si hay un problema con Java. El proyecto establece Java 25 como criterio del daemon.
5. Crear un dispositivo virtual desde **Device Manager**, o conectar un teléfono con depuración USB habilitada y autorizarlo.
6. Seleccionar el módulo **app**, elegir el dispositivo y ejecutar **Run**.

Android Studio genera `local.properties` con la ruta del SDK de cada equipo. Ese archivo está excluido del repositorio. No se necesita configurar un servidor ni una cuenta externa.

## Recorrido para probar cámara y persistencia

1. Seleccionar **Ángel Quintero** y entrar.
2. Abrir **Nuevo documento**, ingresar un título y seleccionar un tipo.
3. Pulsar **Escanear**, fotografiar una página y confirmar la captura en la cámara.
4. Comprobar la vista previa y guardar el documento. Si la captura se cancela, se conserva el adjunto anterior.
5. Abrir el adjunto desde el detalle para comprobar la imagen guardada.
6. Cerrar la aplicación, finalizar su proceso y volver a abrirla. Seleccionar el mismo funcionario y verificar que el documento y su imagen siguen disponibles.
7. Cerrar sesión desde el avatar, seleccionar **Dr. Pedro Soto**, pulsar **Verificar identidad y entrar** y completar el diálogo biométrico de Android.
8. Abrir el documento pendiente y aprobarlo o rechazarlo, indicando una observación para el rechazo.
9. Volver a la bandeja del doctor y seleccionar **Aprobados** o **Rechazados** para consultar sus decisiones. Abrir un documento para ver su detalle, archivo e historial.
10. Volver a cerrar y abrir la app; comprobar el estado y el historial con el perfil del funcionario.

También puede probarse **Elegir archivo** con un PDF, JPG o PNG. La captura actual digitaliza una página como imagen: no incluye OCR, recorte automático ni generación de PDF multipágina.

### Reiniciar la demostración

Desde cualquier perfil, abrir **avatar → Mi perfil → Reiniciar datos locales** y confirmar.

El reinicio afecta a **todos los perfiles del dispositivo**: elimina los documentos añadidos, los adjuntos privados, las decisiones registradas y el formulario activo. Restaura los cinco documentos de ejemplo con sus estados originales y vuelve a la pantalla de acceso. Si se cancela la confirmación, no se modifica nada. No elimina archivos originales seleccionados desde otras ubicaciones del dispositivo.

Las bandejas de decisiones muestran únicamente los documentos del centro revisados por el perfil activo. Los documentos pendientes se retiran de esa pestaña al registrar una decisión, pero permanecen disponibles en **Aprobados** o **Rechazados**. Estas decisiones se recuperan desde SQLite al volver a abrir la app.

## Acceso biométrico del revisor

Al seleccionar **Dr. Pedro Soto**, el acceso utiliza `BiometricPrompt` de AndroidX con `BIOMETRIC_STRONG` (clase 3). En un teléfono con lector de huellas se muestra la verificación del sistema; Android también puede admitir otras modalidades fuertes compatibles, como reconocimiento facial de clase 3. La API no permite seleccionar exclusivamente una modalidad biométrica.

- El botón **Verificar identidad y entrar** abre la sesión tras un resultado biométrico correcto.
- Un intento no reconocido permite reintentar dentro del diálogo; cancelar, un error o el bloqueo del sensor mantienen el acceso cerrado.
- El diálogo biométrico no habilita PIN ni contraseña como métodos alternativos.
- Si no hay biometría registrada, la pantalla ofrece **Configurar biometría** para abrir los ajustes de Android. Sin sensor compatible, la verificación biométrica no está disponible; los funcionarios siguen disponibles.
- Cerrar sesión o iniciar un nuevo proceso finaliza la sesión activa. Una rotación de pantalla conserva el intento activo mediante el ViewModel y la conexión de callbacks de AndroidX.

La validación corresponde a **alguna biometría registrada en el dispositivo**, no identifica por sí misma al Dr. Pedro Soto. Los perfiles siguen siendo académicos. La app no recibe ni guarda imágenes, plantillas o identificadores de huellas. Este control protege la entrada local al perfil; no reemplaza una identidad institucional ni cifra la base SQLite.

### Demostración en un teléfono

1. Configurar un bloqueo de pantalla y registrar una huella compatible en los ajustes de seguridad del teléfono.
2. Ejecutar la app y seleccionar **Dr. Pedro Soto**.
3. Pulsar **Verificar identidad y entrar** y presentar la huella cuando Android lo solicite.
4. Cerrar sesión y repetir, esta vez cancelando: la bandeja del revisor debe permanecer inaccesible.

### Demostración en el emulador

1. Utilizar un AVD con lector de huellas virtual y registrar una huella desde los ajustes de seguridad de Android.
2. Durante el registro, simular las lecturas desde **Extended controls → Fingerprint → Touch Sensor**. Si esos controles no aparecen en la vista integrada, ejecutar el emulador en una ventana independiente.
3. Volver a la app, seleccionar al revisor y activar el mismo dedo virtual cuando aparezca el diálogo.

También puede enviarse una lectura desde una terminal con `adb` configurado:

```bash
adb -e emu finger touch 1
```

El identificador debe corresponder a una huella previamente registrada en ese AVD. El comando simula el sensor del emulador y no constituye una ruta alternativa dentro de la aplicación.

## Compilación y pruebas

Desde una terminal en la raíz, con Java y el SDK configurados:

**Windows / PowerShell**

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

**macOS / Linux**

```bash
bash ./gradlew :app:assembleDebug
bash ./gradlew :app:testDebugUnitTest
bash ./gradlew :app:lintDebug
```

APK de depuración: `app/build/outputs/apk/debug/app-debug.apk`.

Las pruebas incluyen reglas de acceso y revisión, reapertura de SQLite, recuperación de adjuntos e historial, reinicio de la demostración, fallo de escritura, captura JPEG y validación de archivos. Se añaden pruebas del acceso biométrico: rechazo del acceso directo al revisor, intentos fallidos, cancelaciones, resultados tardíos, cambio de perfil, cierre de sesión y sensor simulado en Android 8 con o sin huellas registradas. Las pruebas de integración local utilizan Robolectric; la toma real de fotografías y el recorrido visual deben verificarse en un teléfono o emulador.

Verificación de la versión 0.3.0 realizada el 23 de octubre de 2026 en Windows con JBR 25:

| Comprobación | Resultado |
| --- | --- |
| `assembleDebug` | Correcto; APK generado. |
| `testDebugUnitTest` | 40 pruebas, sin fallos ni errores. |
| `lintDebug` | Sin errores; 16 advertencias. |
| Captura real y recorrido visual | Pendientes de prueba en un dispositivo. |

## Limitaciones y siguientes etapas

- La sesión de demostración se elige nuevamente al iniciar un proceso nuevo.
- Cada documento admite un solo adjunto de hasta 10 MiB.
- La captura no incluye OCR, detección de bordes, corrección de perspectiva ni documentos multipágina.
- Los PDF protegidos con contraseña no se abren en el visor interno.
- Autenticación institucional, SharePoint, correo y sincronización entre dispositivos quedan fuera de esta versión.

## Consideraciones del entorno

- **Emulador:** requiere virtualización y aceleración compatibles con el equipo. Puede utilizarse un teléfono Android con depuración USB como alternativa.
- **Rutas con tildes en Windows:** el proyecto incluye `android.overridePathCheck=true` y `gradle/windows-test-paths.gradle`. La caché de Gradle debe estar en una ruta sin tildes. El ajuste no se ha validado con configuration cache.
- **Git:** las cachés, compilaciones, configuración personal del SDK y documentación de trabajo están excluidas mediante `.gitignore`.

## Tecnologías

| Tecnología | Versión o configuración |
| --- | --- |
| Android Gradle Plugin | 9.1.1 |
| Gradle Wrapper | 9.3.1 |
| Kotlin y plugin Compose | 2.2.10; Kotlin integrado en AGP |
| Jetpack Compose BOM | 2025.10.00 |
| Material Design | Material 3 |
| Activity Compose | 1.11.0 |
| Lifecycle / ViewModel | 2.9.4 |
| Navigation Compose | 2.9.5 |
| Kotlin Coroutines | 1.10.2 |
| AndroidX ExifInterface | 1.4.2 |
| AndroidX Biometric | 1.1.0 |
| AndroidX Fragment | 1.8.9 |
| Base local | SQLite mediante SQLiteOpenHelper |
| Pruebas | JUnit 4.13.2 y Robolectric 4.17 |
| SDK de compilación / destino / mínimo | 37.0 / 36 / 26 |

Las dependencias se centralizan en `gradle/libs.versions.toml`.

## Referencias técnicas

- [Persistencia con SQLite en Android](https://developer.android.com/training/data-storage/sqlite).
- [Contrato de captura TakePicture](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.TakePicture).
- [Configuración de pruebas Robolectric](https://robolectric.org/getting-started/).

- [Diálogo biométrico de Android](https://developer.android.com/identity/sign-in/biometric-auth).
- [Controles del sensor de huella del emulador](https://developer.android.com/studio/run/emulator-extended-controls).
