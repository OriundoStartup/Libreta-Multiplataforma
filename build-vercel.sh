#!/bin/bash
set -e

# --- 1. VALIDACIÓN DE AMBIENTE ---
echo "--- [SRE] Iniciando Build de Supervivencia de Recursos ---"

# Memoria limitada para Node.js
export NODE_OPTIONS="--max-old-space-size=1024"

# --- 2. CONFIGURACIÓN DE JAVA 17 ---
mkdir -p /tmp/jdk17
curl -sL "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz" \
  | tar -xz -C /tmp/jdk17 --strip-components=1

export JAVA_HOME=/tmp/jdk17
export PATH="$JAVA_HOME/bin:$PATH"

# --- 3. CONFIGURACIÓN DE GRADLE ---
chmod +x gradlew

# Crear un archivo de propiedades ULTRA-LIMPIO para Vercel
# Reducimos los workers a 1 y bajamos el metaspace para liberar RAM para Binaryen
cat <<EOF > gradle.properties
org.gradle.jvmargs=-Xmx2560m -XX:MaxMetaspaceSize=384m -XX:+UseSerialGC
org.gradle.daemon=false
org.gradle.parallel=false
org.gradle.workers.max=1
kotlin.incremental=false
kotlin.compiler.execution.strategy=in-process
EOF

echo "--- [SRE] Ejecutando compilación Wasm con perfil de BAJA MEMORIA ---"
# Eliminamos la optimización agresiva via CLI para dejar que build.gradle.kts
# use los flags --low-memory configurados.
./gradlew :composeApp:wasmJsBrowserDistribution --stacktrace --info

echo "--- [SRE] Build completado con éxito ---"
