# Audit & Optimization: Vercel, Supabase & AWS Removal

This plan normalizes the project for Vercel (Wasm) and Supabase, removing legacy AWS branding and optimizing configurations.

## Proposed Changes

### Project Branding
Removing "Aws" from the project identity.

#### [settings.gradle.kts](file:///C:/Users/USUARIO/Downloads/LibretaMultiplataformAws/LibretaMultiplataformAws/settings.gradle.kts)
- Rename `rootProject.name` to `"LibretaMultiplataforma"`.

---

### Vercel Deployment
Optimizing headers and build configuration for Wasm.

#### [vercel.json](file:///C:/Users/USUARIO/Downloads/LibretaMultiplataformAws/LibretaMultiplataformAws/vercel.json)
- Refine security and caching headers for `.wasm` and `.js` files.
- Ensure proper MIME types for Wasm modules.

---

### Backend Normalization (Supabase)
Ensuring all features use Supabase and not AWS.

#### [SupabaseConfig.kt](file:///C:/Users/USUARIO/Downloads/LibretaMultiplataformAws/LibretaMultiplataformAws/shared/src/commonMain/kotlin/com/tuapp/libreta/data/remote/SupabaseConfig.kt)
- Standardize secret access via `BuildKonfig`.

## Verification Plan

### Automated Build
- Run `./gradlew :composeApp:wasmJsBrowserDistribution` to verify the Wasm build still works after renaming.

### Manual Verification
- Check `settings.gradle.kts` for the new name.
- Review `vercel.json` for optimized headers.
- Verify no AWS dependencies are present in `libs.versions.toml`.
