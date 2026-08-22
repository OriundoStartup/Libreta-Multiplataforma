# Libreta Multiplataforma 🚀

Una solución robusta y moderna para la gestión académica, construida con **Kotlin Multiplatform (KMP)** y **Jetpack Compose**, diseñada para ofrecer una experiencia fluida en Android, iOS, Web (Wasm) y Desktop.

---

## 🌟 Estado Actual del Sistema

El sistema ha sido recientemente **estabilizado y blindado**, logrando una paridad de funciones y una robustez excepcional en todos sus objetivos de compilación.

### Hitos Recientes:
- **Estabilización de Wasm (Web)**: Secuenciación de inicialización de base de datos (`dbReady`) implementada para garantizar disponibilidad de tablas en la carga inicial.
- **Normalización de Dominio**: Modelos de datos (`Student`, `Attendance`, `Justification`) optimizados y normalizados (3NF) para asegurar integridad en la persistencia.
- **Blindaje de Persistencia**: Implementación de políticas de RLS (Row Level Security) en Supabase y esquemas SQLDelight sincronizados.
- **Suite de Pruebas al 100%**: Cobertura de tests unitarios validada en Android, iOS y Wasm, asegurando que la lógica de negocio es idéntica en todas las plataformas.

---

## 🛠️ Stack Tecnológico

| Componente | Tecnología |
| :--- | :--- |
| **UI Framework** | [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) |
| **Backend / Auth** | [Supabase](https://supabase.com/) (Auth, Database, Storage) |
| **Local Database** | [SQLDelight](https://cashapp.github.io/sqldelight/) |
| **Dependency Injection** | [Koin](https://insert-koin.io/) |
| **Navigation** | [Voyager](https://voyager.adriel.cafe/) |
| **Coroutines / Flows** | [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines) |
| **Deployment** | [Vercel](https://vercel.com/) (Web Target) |

---

## 🏗️ Arquitectura

El proyecto sigue una arquitectura limpia orientada al dominio (**Clean Architecture**):

- **`:shared`**: El corazón del proyecto.
  - `domain`: Modelos de negocio, interfaces de repositorios y Casos de Uso (Use Cases).
  - `data`: Implementaciones de repositorios, integración con Supabase (Remote) y SQLDelight (Local).
  - `presentation`: ScreenModels (ViewModels) que gestionan el estado de la UI de forma reactiva.
  - `ui`: Componentes y pantallas compartidas de Compose.
- **`composeApp`**: Módulo de aplicación que configura los puntos de entrada para cada plataforma.

---

## 📱 Soporte de Plataformas

### Android
- Compilación optimizada con soporte para `SyncManager`.
- Integración nativa con esquemas de seguridad de red.

### iOS
- Integración vía CocoaPods / Swift Package Manager.
- UI 100% compartida en SwiftUI.

### Web (Wasm)
- **Tecnología de Vanguardia**: Compilado a WebAssembly para un rendimiento cercano al nativo.
- Desplegado automáticamente en Vercel.

### Desktop (JVM)
- Soporte para Windows, macOS y Linux.

---

## 🧪 Pruebas Unitarias

Para ejecutar la suite completa de pruebas en todas las plataformas:

```powershell
.\gradlew :shared:allTests
```

Las pruebas cubren:
- Lógica de sincronización de datos.
- Validación de formatos (UUIDs, RUT, Fechas).
- Comportamiento de los ScreenModels y estados de UI.
- Integración de casos de uso.

---

## 🚀 Despliegue Local

### Web (Wasm)
```powershell
.\gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

### Android
```powershell
.\gradlew :composeApp:assembleDebug
```

---

## 📝 Notas de Desarrollo
Este proyecto utiliza un sistema de **UUIDs Estrictos** para asegurar la integridad referencial. Todas las IDs de dominio están envueltas en la clase `UuidString` que valida el formato en tiempo de ejecución en todas las plataformas.