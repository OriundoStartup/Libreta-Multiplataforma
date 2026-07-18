#!/bin/bash
set -e

# --- 1. VALIDACIÓN DE AMBIENTE ---
echo "--- [SRE] Validación de Recursos ---"
free -m || echo "Comando 'free' no disponible"

if [ -z "$SUPABASE_URL" ] || [ -z "$SUPABASE_KEY" ]; then
  echo "ERROR: Las variables SUPABASE_URL o SUPABASE_KEY no están configuradas en Vercel."
  exit 1
fi

# Aumentamos la memoria de Node.js. Gradle descarga Node internamente para empaquetar Wasm/JS.
# Si Node se queda sin RAM durante Webpack, Gradle falla silenciosamente.
export NODE_OPTIONS="--max-old-space-size=3072"

# --- 2. CONFIGURACIÓN DE JAVA ---
echo "--- [SRE] Instalando Amazon Corretto 17 ---"
mkdir -p /tmp/jdk17
curl -sL "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz" \
  | tar -xz -C /tmp/jdk17 --strip-components=1

export JAVA_HOME=/tmp/jdk17
export PATH="$JAVA_HOME/bin:$PATH"

echo "Java version:"
java -version

# --- 3. CONSTRUCCIÓN ---
echo "--- [SRE] Iniciando Compilación Gradle (Wasm Optimized) ---"
chmod +x gradlew

# CAMBIOS CLAVE:
# 1. Subimos -Xmx a 3072m (3GB). Vital para el compilador Wasm.
# 2. MaxMetaspaceSize a 512m (Compose genera muchas clases en tiempo de compilación).
# 3. Quitamos --info para no saturar el buffer de logs de Vercel.
./gradlew :composeApp:wasmJsBrowserDistribution \
  --no-daemon \
  --stacktrace \
  --max-workers=1 \
  --no-configuration-cache \
  -Dorg.gradle.jvmargs="-Xmx3072m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC" \
  -Dfile.encoding=UTF-8

echo "--- [SRE] Despliegue completado exitosamente ---"
