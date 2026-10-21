# CMQ Salud — Gestión documental

Aplicación Android para demostrar el registro, consulta y revisión de documentos laborales y administrativos de funcionarios de CMQ Salud. Desarrollada con **Kotlin, Jetpack Compose y arquitectura MVVM** como proyecto académico de DSY1105.

| Información académica | Detalle |
| --- | --- |
| Integrantes | Ángel Quintero, Food Fontus y Máximo Lugo |
| Sección | 004-D |
| Docente | Bastian Lisperguer |
| Versión | 0.1.0 — Base navegable de demostración |

## Objetivo y estado actual

El proyecto busca facilitar la organización y el seguimiento de antecedentes documentales mediante dos perfiles: funcionario y revisor. Esta versión implementa el flujo inicial con cinco pantallas conectadas, formularios con validación y cambios de estado visibles en la interfaz.

Todos los usuarios y documentos incluidos son ficticios. El acceso se realiza mediante selección de perfiles de demostración, sin credenciales institucionales.

**Los datos se mantienen en memoria:** los registros y las decisiones se conservan al cambiar de perfil durante la misma ejecución, pero se restablecen al finalizar el proceso de la aplicación. La persistencia local todavía no está implementada.

## Funcionalidades disponibles

- Selección de perfil de funcionario o revisor.
- Listado de documentos con búsqueda por título y filtros por tipo y estado.
- Registro de un documento con título, tipo, observación y adjunto simulado.
- Validación del formulario y mensajes de error o confirmación.
- Consulta del detalle, estado e historial de un documento.
- Bandeja de documentos pendientes para el revisor.
- Aprobación o rechazo de documentos; el rechazo requiere una observación.
- Actualización de listados y detalles mediante estado observable.
- Navegación entre pantallas y cierre de la sesión de demostración.

El repositorio limita la consulta del funcionario a sus propios documentos y la del revisor a los documentos de otros titulares de su centro. Estas restricciones corresponden al prototipo académico y no constituyen autenticación ni autorización de un servicio institucional.

## Arquitectura MVVM

La aplicación separa la presentación, el estado de la interfaz y el acceso a los datos:

```text
Acción del usuario
       ↓
Interfaz Compose (ui)
       ↓ evento
CmqViewModel (viewmodel)
       ↓ consulta u operación
DocumentoRepository → DemoDocumentoRepository (repository)
       ↓ datos y resultados mediante StateFlow
CmqViewModel → nuevo estado → interfaz actualizada
```

| Capa | Responsabilidad |
| --- | --- |
| `ui/` | Presentar el estado, recibir acciones y enviarlas mediante callbacks. Incluye pantallas, componentes, tema y navegación. |
| `viewmodel/` | Gestionar el perfil activo, filtros, estado del formulario, validaciones y procesamiento de eventos. Expone estado mediante `StateFlow`. |
| `model/` | Definir usuarios, documentos, adjuntos de demostración, revisiones, eventos y estados. |
| `repository/` | Centralizar el acceso a los datos y las restricciones de consulta y revisión. La implementación actual utiliza memoria. |

`MainActivity` inicia Compose y conecta el ViewModel con la interfaz. `CmqApplication` proporciona una instancia compartida del repositorio. `CmqApp` contiene el `NavHost` y mantiene el `NavController` dentro de la capa de interfaz.

La base utiliza un ViewModel compartido por el flujo. El contrato `DocumentoRepository` separa las pantallas de la implementación de almacenamiento y permite incorporar posteriormente una fuente persistente.

### Estructura del código

```text
app/src/main/java/cl/cmq/salud/
├── MainActivity.kt
├── CmqApplication.kt
├── ui/
│   ├── CmqApp.kt
│   ├── access/             # Acceso de demostración
│   ├── documents/          # Listado, búsqueda y filtros
│   ├── documentform/       # Registro de documentos
│   ├── documentdetail/     # Detalle, historial y revisión
│   ├── review/             # Bandeja de pendientes
│   ├── components/         # Componentes reutilizables
│   └── theme/              # Estilo visual
├── viewmodel/
├── model/
└── repository/
```

## Requisitos para ejecutar el proyecto

- Android Studio compatible con Android Gradle Plugin 9.1.1.
- JDK 25 para el daemon de Gradle, según `gradle/gradle-daemon-jvm.properties`. La compilación de referencia utilizó JBR 25 de Android Studio.
- Android SDK Platform **37.0** y Android SDK Build Tools **36.0.0**.
- Emulador o dispositivo físico con **Android 8.0 / API 26 o superior**.
- Conexión a Internet durante la primera sincronización para descargar las herramientas y dependencias necesarias.

El repositorio incluye Gradle Wrapper; no requiere instalar Gradle por separado. El bytecode de la aplicación se configura para Java/Kotlin 17, independientemente del JDK usado por el daemon.

## Instalación y ejecución en Android Studio

1. Clonar el repositorio. La base inicial está disponible en la rama `dev-angel`; seleccionar esa rama para probar esta entrega.
2. Abrir en Android Studio la carpeta raíz que contiene `settings.gradle.kts` y `gradlew`, en lugar de abrir solamente `app/`.
3. Instalar los componentes del SDK indicados desde **SDK Manager** y esperar a que termine la sincronización de Gradle.
4. Revisar la configuración de Gradle JDK si la sincronización informa un problema con Java. El proyecto establece Java 25 como criterio del daemon.
5. Crear un dispositivo virtual desde **Device Manager**, o conectar un teléfono con depuración USB habilitada y autorizar la conexión.
6. Seleccionar el módulo **app**, elegir el dispositivo y ejecutar **Run**.

La pantalla inicial permite seleccionar un perfil de demostración. No se necesita una cuenta externa ni configurar un servidor.

Android Studio genera `local.properties` con la ruta del SDK de cada equipo. Ese archivo está excluido del repositorio y no debe copiarse desde otro entorno.

## Recorrido de demostración

1. Ingresar como **Funcionario Demo A** y consultar el listado inicial.
2. Probar la búsqueda, los filtros y la navegación hacia el detalle de un documento.
3. Abrir **Nuevo documento**, ingresar un título y seleccionar un tipo.
4. Utilizar **Elegir archivo → Usar ejemplo** para adjuntar el PDF simulado y guardar. El formulario muestra errores si faltan datos obligatorios o el título no cumple la longitud permitida.
5. Consultar el detalle del registro creado, inicialmente pendiente de revisión.
6. Salir y seleccionar **Revisor Demo A**, sin finalizar el proceso de la aplicación.
7. Abrir la bandeja de pendientes, seleccionar el documento y aprobarlo o rechazarlo. Para rechazar, ingresar una observación.
8. Volver al perfil del funcionario y comprobar el cambio de estado y el historial.

Los perfiles **Funcionario Demo B** y **Funcionario Demo C** permiten comprobar la separación de expedientes y centros. El funcionario C pertenece a un centro distinto del revisor A.

## Compilación y pruebas

Desde una terminal en la raíz del proyecto, con Java y el SDK configurados:

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

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

Las pruebas unitarias del repositorio verifican el aislamiento entre funcionarios, la restricción por centro, el rechazo con observación obligatoria, la protección de decisiones ya registradas y la visibilidad de nuevos documentos según el perfil.

### Verificación registrada para la base inicial

| Comprobación | Resultado registrado |
| --- | --- |
| Compilación de depuración | Correcta; APK generado |
| Pruebas unitarias | 5 pruebas, sin fallos ni errores |
| Android Lint | Sin errores; 12 avisos |
| Ejecución y recorrido manual en dispositivo | Pendiente de verificación |

La verificación inicial se realizó en Windows con JBR 25 y Gradle 9.3.1. La validación visual y del flujo completo requiere ejecutar la aplicación en un dispositivo o emulador disponible.

## Alcance pendiente

| Funcionalidad | Situación |
| --- | --- |
| Persistencia con Room | Prevista para el MVP local; todavía no implementada. |
| Cámara, selección y visualización de archivos reales | Pendientes. Los adjuntos actuales son ejemplos simulados. |
| Historial persistente | Pendiente; el historial actual se mantiene en memoria. |
| Autenticación institucional | Fuera de esta versión. |
| SharePoint, correo y sincronización entre dispositivos | Fuera de esta versión. |

Los botones relacionados con archivos reales o cámara informan su estado de implementación. La base no realiza capturas, transferencias ni almacenamiento de documentos reales.

## Consideraciones del entorno

- **Emulador:** requiere una configuración de virtualización y aceleración compatible con el equipo. Como alternativa, puede utilizarse un teléfono Android con depuración USB.
- **Rutas con tildes en Windows:** el proyecto incluye `android.overridePathCheck=true` y un ajuste en `gradle/windows-test-paths.gradle` para ejecutar las pruebas cuando la ruta contiene caracteres no ASCII. La caché de Gradle debe estar en una ruta sin tildes. El ajuste no se ha validado con configuration cache.
- **Archivos locales:** las cachés, compilaciones, configuración personal del SDK y documentación de trabajo están excluidas mediante `.gitignore`.

## Tecnologías y versiones

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
| Pruebas | JUnit 4.13.2 |
| SDK de compilación / destino / mínimo | 37.0 / 36 / 26 |

Las versiones se centralizan en `gradle/libs.versions.toml`. La configuración utiliza Kotlin integrado en AGP, por lo que no aplica adicionalmente el plugin `org.jetbrains.kotlin.android`.
