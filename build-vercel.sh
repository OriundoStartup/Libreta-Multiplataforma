#!/bin/bash
set -e

# --- 1. VALIDACIÓN DE AMBIENTE ---
echo "--- [SRE] Validación de Recursos ---"
free -m || echo "Comando 'free' no disponible"

if [ -z "$SUPABASE_URL" ] || [ -z "$SUPABASE_KEY" ]; then
  echo "ERROR: Las variables SUPABASE_URL o SUPABASE_KEY no están configuradas en Vercel."
  exit 1
fi

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
echo "--- [SRE] Iniciando Compilación Gradle (Optimized Mode) ---"
chmod +x gradlew

# Ejecutamos con límites estrictos de memoria para evitar OOM Killer en Vercel
./gradlew :composeApp:wasmJsBrowserDistribution \
  --no-daemon \
  --stacktrace \
  --info \
  --max-workers=1 \
  --no-configuration-cache \
  -Dorg.gradle.jvmargs="-Xmx1536m -XX:MaxMetaspaceSize=384m" \
  -Dfile.encoding=UTF-8

echo "--- [SRE] Despliegue completado exitosamente ---"
