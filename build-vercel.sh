#!/bin/bash
set -e

echo "--- [SRE] Iniciando Build de Supervivencia v8 (Isolation + Artifact Export) ---"

# 1. AISLAMIENTO DE DIRECTORIOS
# Forzamos a Gradle y Android a usar /tmp, que es 100% escribible en Vercel
mkdir -p /tmp/.gradle
mkdir -p /tmp/.android
export GRADLE_USER_HOME="/tmp/.gradle"
export ANDROID_USER_HOME="/tmp/.android"
export ANDROID_HOME="/tmp"

# 2. CONFIGURACIÓN DE JAVA 17
mkdir -p /tmp/jdk17
if [ ! -f "/tmp/jdk17/bin/java" ]; then
    curl -sL "https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz" \
      | tar -xz -C /tmp/jdk17 --strip-components=1
fi
export JAVA_HOME=/tmp/jdk17
export PATH="$JAVA_HOME/bin:$PATH"

# 3. CONFIGURACIÓN DE GRADLE
chmod +x gradlew

# org.gradle.daemon=false es vital para CI
cat <<EOF > gradle.properties
org.gradle.jvmargs=-Xmx2560m -XX:MaxMetaspaceSize=512m -Djava.io.tmpdir=/tmp -Duser.home=/tmp -XX:+UseSerialGC
org.gradle.daemon=false
org.gradle.parallel=false
org.gradle.workers.max=1
org.gradle.vfs.watch=false
kotlin.incremental=false
kotlin.compiler.execution.strategy=in-process
android.useAndroidX=true
EOF

echo "--- [SRE] Ejecutando compilación... ---"
./gradlew :composeApp:wasmJsBrowserDistribution \
  --no-daemon \
  --no-build-cache \
  --stacktrace \
  --console=plain \
  --info

# 4. EXPORTACIÓN DE ARTEFACTOS
echo "--- [SRE] Preparando carpeta dist ---"
mkdir -p dist

# Intentar producción, si no existe, usar development
if [ -d "composeApp/build/dist/wasmJs/productionExecutable" ]; then
    cp -r composeApp/build/dist/wasmJs/productionExecutable/* dist/
    echo "Dist listo (Producción)"
else
    cp -r composeApp/build/dist/wasmJs/developmentExecutable/* dist/
    echo "Dist listo (Desarrollo - Fallback)"
fi

# El worker debe estar siempre en la raíz
cp composeApp/src/wasmJsMain/resources/sqldelight-worker.js dist/ 2>/dev/null || true

echo "--- [SRE] Build Finalizado con éxito ---"
ls -lh dist
