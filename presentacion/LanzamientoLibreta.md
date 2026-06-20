---
marp: true
theme: default
paginate: true
size: 16:9
header: "Libreta Multiplataforma · Lanzamiento"
footer: "© 2026 OriundoStartup"
---

<!-- _class: lead -->

# Libreta Multiplataforma

### La libreta escolar del siglo XXI

**Una sola app. Todas las plataformas. Todos los actores.**

Lanzamiento — Mayo 2026

---

## El problema

Hoy la comunicación escuela ↔ familia sigue siendo:

- **Fragmentada**: WhatsApp, papelitos, correos, llamadas.
- **Opaca**: el apoderado no ve la asistencia ni las notas en tiempo real.
- **Burocrática**: justificar una inasistencia toma días.
- **Costosa**: cada colegio compra 3–4 sistemas distintos que no se hablan.

> En Chile, **8 de cada 10 apoderados** declaran no estar al día con la asistencia de su hijo.

---

## La solución

**Libreta Multiplataforma** es una sola plataforma que conecta:

| Profesor | Apoderado | Establecimiento |
| :--- | :--- | :--- |
| Pasa lista en 30 s | Ve asistencia hoy | Reportes consolidados |
| Envía avisos al curso | Justifica desde el celular | RLS por colegio |
| Registra notas | Recibe notificaciones | Datos normalizados |

Una sola fuente de verdad. **Online y offline.**

---

## Qué incluye la versión 1.0

1. **Asistencia diaria** — registro masivo con un toque por alumno.
2. **Justificaciones** — flujo apoderado → profesor con aprobación.
3. **Notas** — registro individual y masivo por evaluación.
4. **Mensajería** — directa profesor ↔ apoderado.
5. **Avisos de curso** — broadcast del profesor con categorías (urgente, académico, positivo).
6. **Códigos de invitación** — apoderado entra a un curso con un código de 6 dígitos.
7. **Multi-curso** — un apoderado puede tener varios hijos en distintos cursos.

---

## Diferenciadores técnicos

- **Una sola base de código** para Android, iOS, Web y Desktop.
  Cualquier feature que lanzamos llega a los 4 targets en el mismo PR.
- **Web en WebAssembly**, no en JavaScript: rendimiento cercano al nativo.
- **Offline-first**: SQLDelight local + sincronización incremental con Supabase.
- **Seguridad por defecto**: RLS en cada tabla, PKCE en OAuth, UUIDs estrictos.
- **Test suite multiplataforma**: la misma lógica de negocio se valida en los 4 targets.

---

## Stack

| Capa | Tecnología |
| :--- | :--- |
| UI | Compose Multiplatform 1.7 |
| Backend | Supabase (Auth + Postgres + Storage) |
| Persistencia local | SQLDelight 2.3 |
| DI | Koin 4.0 |
| Navegación | Voyager 1.1 |
| Web | Kotlin/Wasm + Vercel |

Stack **moderno, mantenido y open source**. Sin vendor lock-in.

---

## Mercado objetivo

**Fase 1 — Validación (Q2–Q3 2026)**
Colegios particulares subvencionados de la Región Metropolitana,
500–1.500 alumnos.

**Fase 2 — Expansión (Q4 2026)**
Municipales y particulares, regiones V y VIII.

**Fase 3 — Plataforma (2027)**
Apertura de API pública para integraciones (SIGE, Napsis, Webclass).

---

## Modelo de negocio

| Plan | Precio | Para quién |
| :--- | :--- | :--- |
| **Free** | $0 | Hasta 30 alumnos. Sin soporte. |
| **Colegio** | $1.500 / alumno / año | Curso completo, soporte por email. |
| **Sostenedor** | A convenir | Multi-colegio, SSO, on-prem opcional. |

Cobranza anual prepagada por establecimiento. Sin freemium para apoderados.

---

## Roadmap post-lanzamiento

**Q3 2026**
- Notificaciones push (Android + iOS)
- Exportación de planillas a PDF/Excel
- Modo oscuro completo

**Q4 2026**
- Calendario académico con eventos
- Integración con Google Workspace for Education
- Reportes de tendencia (riesgo de deserción)

**Q1 2027**
- API pública v1
- Plugin marketplace

---

## Tracción a la fecha

- ✅ App estable en Android, iOS y Web (Wasm).
- ✅ Suite de tests unitarios pasando en los 4 targets.
- ✅ Backend en Supabase con RLS por colegio.
- ✅ Deploy continuo a Vercel.
- 🚧 3 colegios piloto comprometidos para Q3 2026.

---

<!-- _class: lead -->

## ¿Qué necesitamos ahora?

1. **3 colegios piloto** para Q3 2026 (cerrar el feedback loop real).
2. **Un diseñador UX** para pulir las pantallas de profesor.
3. **Capital para 6 meses** de operación y marketing dirigido.

---

<!-- _class: lead -->

# Gracias

**OriundoStartup**
juan.camposgomez80@gmail.com

[github.com/OriundoStartup/Libreta-Multiplataforma](https://github.com/OriundoStartup/Libreta-Multiplataforma)
