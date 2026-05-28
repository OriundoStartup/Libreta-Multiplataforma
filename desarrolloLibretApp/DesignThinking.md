# Design Thinking: LibretApp

## 1. Concepto
**LibretApp** es un ecosistema digital educativo desarrollado con **Kotlin Multiplatform (KMP)** que conecta de forma transparente a profesores, instituciones y apoderados. Su núcleo se basa en la **"Sincronía Simbiótica"**, permitiendo que la gestión escolar ocurra en tiempo real o diferido (offline), garantizando que la información administrativa nunca se pierda.

---

## 2. ¿Para qué? (Objetivo)
Para eliminar la fragmentación de la información académica y reducir la carga administrativa manual de los docentes, proporcionando a los apoderados una ventana inmediata al progreso, asistencia y bienestar de sus hijos, fomentando una comunidad educativa proactiva y no reactiva.

---

## 3. ¿Por qué? (Justificación)
*   **Brecha de Comunicación:** Los métodos tradicionales (libretas de papel, correos, WhatsApp informales) causan pérdida de información crítica.
*   **Carga Administrativa:** Los docentes gastan hasta un 30% de su tiempo en registros duplicados.
*   **Falta de Datos en Tiempo Real:** Las instituciones suelen detectar el riesgo de deserción o reprobación cuando ya es demasiado tarde para intervenir.
*   **Resiliencia Tecnológica:** El uso de KMP asegura que la app sea accesible en Android, iOS y Web con una única lógica de negocio, optimizando costos de mantenimiento.

---

## 4. ¿Qué DEBE hacer? (Scope)
*   **Asistencia Offline-First:** El profesor debe poder pasar lista en el aula sin depender de la señal de Wi-Fi, con sincronización automática posterior.
*   **Gestión de Calificaciones:** Registro masivo de notas, cálculo de promedios ponderados y visualización de tendencias.
*   **Comunicación Oficial:** Sistema de mensajería privada y avisos (circulares) con acuse de recibo.
*   **Justificaciones Digitales:** Flujo de trabajo para que el apoderado gestione inasistencias cargando evidencias (fotos/documentos).
*   **Reportes de Alerta Temprana:** Dashboard de analíticas que resalte alumnos con baja asistencia o notas bajo el umbral.
*   **Arquitectura Multi-Colegio:** Soporte nativo para que un docente o padre pueda alternar entre distintas instituciones.

---

## 5. ¿Qué NO debe hacer? (Out of Scope)
*   **Red Social Abierta:** No permite chats entre apoderados ni publicaciones de muro de estilo recreativo.
*   **LMS Pesado:** No es un reemplazo de Moodle o Google Classroom para subir videos de clases o tareas de gran tamaño.
*   **Gestión Contable:** No procesa pagos de matrículas ni mensualidades (en la fase actual).
*   **Entretenimiento:** No incluye elementos de gamificación ajenos al registro académico formal.
