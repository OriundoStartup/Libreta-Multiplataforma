# Diseño Frontend Multiplataforma: LibretApp

## 1. Arquitectura de UI Compartida
Utilizamos **Compose Multiplatform** con una arquitectura **MVI (Model-View-Intent)** simplificada mediante `ScreenModels` de Voyager.
*   **Diseño:** Material Design 3 (M3) con sistema de colores dinámicos.
*   **Layout:** Sistema basado en `WindowSizeClass` (Compact, Medium, Expanded).

---

## 2. Estrategia por Plataforma

### A. Android (Nativo-Like)
*   **Navegación:** Integración con el botón "Atrás" físico/gestual del sistema.
*   **Interacciones:** Ripple effects nativos, soporte para Modo Oscuro del sistema.
*   **Deep Linking:** Manejo de `org.oriundo://login-callback` mediante `IntentFilters` en el `AndroidManifest.xml`.
*   **Performance:** Uso de `R8/ProGuard` para optimizar el APK y reducir el tamaño del runtime de Kotlin.

### B. iOS (Shared-UI en UIKit)
*   **Contenedor:** La UI de Compose se encapsula en un `ComposeUIViewController` dentro de un `UIWindow`.
*   **Ergonomía:** 
    *   Soporte para **Safe Areas** (evitar notch y barra de inicio).
    *   Simulación de gestos de "Swipe to back" mediante transiciones de Voyager.
    *   Uso de fuentes nativas del sistema si es posible para mejorar la legibilidad.
*   **Build:** Generación de un Framework estático para fácil consumo desde Xcode.

### C. Web (WasmJS / Browser)
*   **Motor:** Renderizado mediante **Skia (CanvasKit)** sobre WebAssembly para máximo rendimiento gráfico.
*   **Navegación (URL Mapping):** 
    *   Implementación de un `WebPathMapper` que sincroniza el estado de Voyager con la barra de direcciones del navegador.
    *   Soporte para botones "Adelante/Atrás" del browser.
*   **Responsive Layout:**
    *   **Desktop View:** Uso de paneles laterales (Side Rails) y grids de múltiples columnas para Dashboards.
    *   **Mouse Interaction:** Estados de `hover` en botones y tarjetas que no existen en touch.
*   **Deployment:** Optimizado para Vercel mediante scripts de build personalizados (`build-vercel.sh`).

---

## 3. Componentes Maestros de UI

| Componente | Descripción | Adaptación |
| :--- | :--- | :--- |
| **TopAppBar** | Título y acciones globales. | En Web es más alta y puede incluir el perfil. |
| **NavigationRail** | Menú lateral para tablets/Desktop. | Solo visible en pantallas `Medium` o `Expanded`. |
| **BottomBar** | Menú inferior para móviles. | Oculta en Web/Desktop para maximizar espacio. |
| **AdaptiveGrid** | Lista de alumnos o cursos. | 1 col en Phone, 2 en Tablet, 3+ en Desktop. |

---

## 4. Gestión de Estado y Navegación
*   **Voyager:** Maneja la pila de pantallas de forma agnóstica a la plataforma.
*   **Koin Compose:** Inyecta los `ScreenModels` en el punto de entrada de cada pantalla.
*   **CollectAsStateWithLifecycle:** Asegura que la UI no consuma recursos cuando la app está en segundo plano (especialmente en Android).

---

## 5. Decisiones de Diseño Visual (Theming)
*   **Tipografía:** Inter para una lectura técnica clara en reportes.
*   **Colores de Estado:**
    *   `Success` (Verde): Asistencia completa / Notas aprobadas.
    *   `Error` (Rojo): Inasistencia / Reprobación.
    *   `Warning` (Amarillo): Justificaciones pendientes.
