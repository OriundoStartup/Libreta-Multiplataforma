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

# Crear un archivo de propiedades LIMPIO para Vercel
# Bajamos el Heap a 3GB para evitar ser matados por el OOM Killer de Vercel
cat <<EOF > gradle.properties
org.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC
org.gradle.daemon=false
org.gradle.parallel=false
org.gradle.workers.max=1
kotlin.incremental=false
kotlin.compiler.execution.strategy=in-process
EOF

echo "--- [SRE] Ejecutando compilación Wasm con optimización ACTIVADA ---"
# Re-activamos la optimización para reducir el tamaño del Wasm (evita crashes en el navegador)
./gradlew :composeApp:wasmJsBrowserDistribution \
  -Pkotlin.wasm.optimization=true \
  --stacktrace

echo "--- [SRE] Build completado con éxito ---"
