# RealmDatabaseManager

Aplicación Android de aprendizaje para gestionar datos con Realm: crear, consultar, modificar y eliminar colecciones, campos y registros.

## Tecnologías y requisitos

La configuración de [app/build.gradle.kts](app/build.gradle.kts) utiliza Kotlin, Jetpack Compose y Realm Kotlin. Declara Java 17, Android SDK 35 y Android 7.0/API 24 como mínimo.

## Abrir y ejecutar

1. Clona el repositorio y abre la carpeta en Android Studio.
2. Configura el SDK de Android y el JDK 17 del proyecto.
3. Sincroniza Gradle.
4. Selecciona un emulador o dispositivo con API 24 o superior y ejecuta el módulo `app`.

Para solicitar una compilación desde PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

En macOS/Linux usa `./gradlew assembleDebug`. El archivo `local.properties` debe apuntar al SDK de tu propio equipo.

## Estructura

| Ruta | Contenido |
| --- | --- |
| [app/](app/) | Código, interfaz y configuración de la aplicación. |
| [gradle/](gradle/) | Wrapper y configuración auxiliar de Gradle. |
| [settings.gradle.kts](settings.gradle.kts) | Repositorios de dependencias y módulos. |

## Qué practico

Operaciones CRUD, persistencia, desarrollo Android e interfaces con Compose. Las dependencias conservan las versiones declaradas por el proyecto; esta documentación no implica una actualización ni una compilación verificada.
