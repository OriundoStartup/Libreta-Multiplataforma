#!/bin/bash
set -e

# --- 1. VALIDACIÓN DE AMBIENTE ---
echo "--- [SRE] Validación de Recursos ---"
free -m || echo "Comando 'free' no disponible"

if [ -z "$SUPABASE_URL" ] || [ -z "$SUPABASE_KEY" ]; then
  echo "ERROR: Las variables SUPABASE_URL o SUPABASE_KEY no están configuradas en Vercel."
  exit 1
fi

# Memoria controlada para Node.js
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
echo "--- [SRE] Iniciando Compilación Gradle (v2.4 Stable) ---"
chmod +x gradlew

# Reintentar la descarga de Gradle si falla (Resiliencia ante error 503)
export GRADLE_OPTS="-Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000"

./gradlew clean :composeApp:wasmJsBrowserDistribution \
  --no-daemon \
  --stacktrace \
  --max-workers=1 \
  --no-configuration-cache \
  -Dorg.gradle.jvmargs="-Xmx2048m -XX:MaxMetaspaceSize=512m -XX:+UseParallelGC" \
  -Dkotlin.daemon.jvm.options="-Xmx1024m" \
  -Dfile.encoding=UTF-8

echo "--- [SRE] Despliegue completado exitosamente ---"
