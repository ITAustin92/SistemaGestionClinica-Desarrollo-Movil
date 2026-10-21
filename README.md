# CMQ Salud

Base Android de Gestión Documental CMQ Salud para DSY1105, sección 004-D, profesor Bastian Lisperguer.

Integrantes: Ángel Quintero, Food Fontus y Máximo Lugo.

## Objetivo de esta versión

Proporcionar un proyecto ejecutable y navegable que represente las cinco vistas acordadas. La aplicación utiliza exclusivamente usuarios y documentos ficticios. La estética parte de los mockups de `Documentacion/05_Mockups`.

Esta es la base de interfaz v0.1.0. No es todavía el MVP completo descrito en la documentación: los datos viven **en memoria** y se restablecen cuando termina el proceso. Cambiar de perfil dentro de la misma ejecución conserva los cambios de demostración.

## Abrir en Android Studio

1. Seleccionar **Open** y abrir esta carpeta raíz, donde está `settings.gradle.kts`. No abrir únicamente `app/`.
2. Esperar a que termine la sincronización de Gradle. La primera vez necesita Internet para descargar dependencias.
3. Usar el JDK integrado de Android Studio (JBR 25 en el equipo actual) o un JDK compatible con Gradle 9.3.1. En **Settings → Build, Execution, Deployment → Build Tools → Gradle**, seleccionar el Gradle JDK correspondiente.
4. Instalar **Android SDK Platform 37.0** y **Build Tools 36.0.0** desde SDK Manager si faltan. Ya están instalados en el equipo de preparación.
5. Seleccionar la configuración **app** y un teléfono/emulador Android API 26 o superior. En este equipo existe `Medium_Phone_API_37.0`.
6. Presionar **Run**. La pantalla inicial es Acceso de demostración.

`local.properties` contiene la ruta del SDK de este equipo y está excluido de Git. En otro equipo Android Studio debe generar su propia ruta. No cambiar el SDK de todos los integrantes a una ruta personal.

## Recorrido disponible

- Seleccionar **Funcionario Demo A** y entrar.
- Consultar documentos, buscar por título y filtrar por tipo/estado.
- Abrir detalle y regresar al listado.
- Pulsar **Nuevo documento**, ingresar título y tipo; **Elegir archivo → Usar ejemplo** adjunta un PDF simulado.
- Guardar permite comprobar campos obligatorios, estado de formulario, validación, respuesta visible y navegación al detalle. El registro se crea solo en memoria.
- Salir y entrar como **Revisor Demo A** para revisar pendientes del Centro Demo A.
- Abrir un pendiente; aprobar o rechazar. Rechazar exige observación. La decisión actualiza el estado y el historial de demostración.
- Salir y regresar como funcionario para ver la decisión durante esa misma ejecución.

También hay funcionarios ficticios B y C para comprobar que cada uno ve su expediente y que el revisor no consulta otro centro.

## Qué está pendiente

- Persistencia Room y recuperación después de terminar el proceso.
- Selector de archivos reales, cámara y visor del adjunto. Sus botones explican que son acciones previstas; no solicitan permisos ni simulan una captura exitosa.
- Autenticación institucional, SharePoint, correo y sincronización entre dispositivos, fuera del alcance local acordado.
- Auditoría persistente y pruebas completas del MVP. El historial actual solo representa carga y decisión en memoria.

## Arquitectura MVVM

```text
app/src/main/java/cl/cmq/salud/
├── MainActivity.kt              # Inicio de Compose y conexión de dependencias
├── CmqApplication.kt            # Instancia compartida del repositorio demo
├── ui/
│   ├── CmqApp.kt                # NavHost, rutas y conexión de estado/callbacks
│   ├── access/                  # P01 Acceso
│   ├── documents/               # P02 Listado y filtros
│   ├── documentform/            # P03 Formulario
│   ├── documentdetail/          # P04 Detalle y revisión
│   ├── review/                  # P05 Bandeja de pendientes
│   ├── components/              # Botón, campo, selector, tarjeta y estado
│   └── theme/                   # Paleta, tipografía y formas
├── viewmodel/                   # CmqViewModel y estados de UI
├── model/                       # Usuarios, documentos, estados y revisiones
└── repository/                  # Contrato y fuente de datos en memoria
```

Las pantallas reciben datos y callbacks. El ViewModel controla sesión, filtros, validación y eventos. El repositorio comprueba el ámbito del usuario y las transiciones. `NavController` permanece en `ui/`. Un ViewModel compartido por el flujo es suficiente para esta base; las pantallas ya están separadas para poder dividir el estado cuando crezcan.

## Versiones fijadas

| Herramienta | Versión |
| --- | --- |
| Android Gradle Plugin | 9.1.1 |
| Gradle Wrapper | 9.3.1 |
| Kotlin integrado en AGP y plugin Compose | 2.2.10 |
| Compose BOM | 2025.10.00 |
| Material | Material 3, administrado por BOM |
| Activity Compose | 1.11.0 |
| Lifecycle | 2.9.4 |
| Navigation Compose | 2.9.5 |
| SDK compilación / destino / mínimo | 37.0 / 36 / 26 |
| Bytecode Java y Kotlin | 17 |

AGP 9 incorpora Kotlin, por lo que no se aplica además `org.jetbrains.kotlin.android`. No aceptar actualizaciones automáticas de todas las dependencias al abrir el proyecto sin revisar compatibilidad.

## Comandos de verificación

Desde PowerShell en la raíz, configurar `JAVA_HOME` hacia el JDK elegido y ejecutar:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

APK de depuración: `app/build/outputs/apk/debug/app-debug.apk`.

La carpeta `.gradle-user-home/` es una caché local utilizada durante la preparación y no se versiona. Android Studio puede usar su caché normal. No subir `local.properties`, cachés, APK ni archivos del IDE al repositorio.

En Windows, mantener la **caché de Gradle en una ruta sin tildes**, por ejemplo `C:\Users\austi\.gradle` (valor estándar). `android.overridePathCheck=true` permite conservar el proyecto en la carpeta solicitada. El script `gradle/windows-test-paths.gradle` copia automáticamente las clases temporales de pruebas a esa caché cuando la ruta del proyecto contiene tildes, para evitar el fallo de carga de clases del ejecutor de Gradle. No cambia las rutas del código ni del APK. No habilitar configuration cache sin revisar primero este ajuste.

## Emulador en este equipo

Durante la comprobación, el AVD existente informó que falta el controlador de aceleración de Android; el intento de arranque por software también terminó sin crear un dispositivo disponible. Esto debe resolverse antes de ejecutar ese AVD desde Android Studio, o se puede usar un teléfono Android con depuración USB autorizada. No se cambió la virtualización de Windows ni la BIOS. Consultar la [configuración oficial de aceleración del emulador](https://developer.android.com/studio/run/emulator-acceleration#vm-windows).

## Referencias técnicas

- [Compatibilidad de AGP 9.1.1](https://developer.android.com/build/releases/agp-9-1-0-release-notes)
- [Kotlin integrado en AGP](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Catálogo de versiones del ejemplo oficial Android](https://github.com/android/car-codelabs/blob/main/build-a-parked-app/end/gradle/libs.versions.toml)
- [Arquitectura Android](https://developer.android.com/topic/architecture)

El estado de las verificaciones realizadas se registra en `VERIFICACION_BASE.md`.
