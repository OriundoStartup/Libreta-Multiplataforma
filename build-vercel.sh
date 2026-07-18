#!/bin/bash
set -e

# --- 1. VALIDACIÓN DE AMBIENTE ---
echo "--- Iniciando Validación de Ambiente ---"
if [ -z "$SUPABASE_URL" ] || [ -z "$SUPABASE_KEY" ]; then
  echo "ERROR: Las variables SUPABASE_URL o SUPABASE_KEY no están configuradas en Vercel."
  echo "Por favor, configúralas en Project Settings -> Environment Variables."
  exit 1
fi
echo "Variables de entorno validadas con éxito."

# --- 2. CONFIGURACIÓN DE JAVA ---
echo "--- Descargando Amazon Corretto 17 ---"
mkdir -p /tmp/jdk17
curl -sL "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz" \
  | tar -xz -C /tmp/jdk17 --strip-components=1

export JAVA_HOME=/tmp/jdk17
export PATH="$JAVA_HOME/bin:$PATH"

echo "Java version:"
java -version

# --- 3. CONSTRUCCIÓN ---
echo "--- Iniciando compilación Gradle (v2.1) ---"
chmod +x gradlew
./gradlew :composeApp:wasmJsBrowserDistribution \
  --no-daemon \
  --parallel \
  --stacktrace \
  --no-configuration-cache \
  --warning-mode none \
  -Dfile.encoding=UTF-8

echo "--- Despliegue completado exitosamente ---"
