@echo off
echo ========================================
echo  EMERGENCY APK BUILD - KS Expire
echo ========================================
echo.

echo 🚨 SOLUCION DE EMERGENCIA PARA JAVA 21 + GRADLE
echo.

REM Verificar archivos necesarios
if not exist "keystore.properties" (
    echo ❌ ERROR: keystore.properties no encontrado
    pause
    exit /b 1
)

if not exist "ks-expire-release.jks" (
    echo ❌ ERROR: ks-expire-release.jks no encontrado
    pause
    exit /b 1
)

echo ✅ Keystore configurado correctamente
echo.

echo 🔧 OPCIONES DE SOLUCION:
echo.
echo 1️⃣  OPCION 1: Usar Java 17 temporalmente
echo    - Descargar OpenJDK 17 desde: https://adoptium.net/
echo    - Configurar JAVA_HOME a Java 17
echo    - Reiniciar Android Studio
echo.
echo 2️⃣  OPCION 2: Usar Android Studio Build
echo    - File ^> Settings ^> Build ^> Gradle
echo    - Gradle JDK: Cambiar a "Embedded JDK" o Java 17
echo    - Sync Project
echo.
echo 3️⃣  OPCION 3: Build manual con Gradle local
echo    - Instalar Gradle 8.8 globalmente
echo    - Ejecutar: gradle assembleRelease
echo.
echo 4️⃣  OPCION 4: Usar comando directo (si tienes Gradle)
echo.

REM Intentar detectar Gradle local
where gradle >nul 2>&1
if %ERRORLEVEL% == 0 (
    echo ✅ Gradle detectado localmente
    echo 🚀 Ejecutando build directo...
    gradle clean assembleRelease
    if %ERRORLEVEL% == 0 (
        echo.
        echo ✅ BUILD EXITOSO!
        echo 📁 APK generado en: app\build\outputs\apk\release\app-release.apk
        echo.
        pause
        exit /b 0
    ) else (
        echo ❌ Build falló con Gradle local
    )
) else (
    echo ⚠️  Gradle no detectado localmente
)

echo.
echo 📋 RECOMENDACION INMEDIATA:
echo.
echo 1. Descarga OpenJDK 17: https://adoptium.net/temurin/releases/?version=17
echo 2. Instala Java 17
echo 3. En Android Studio:
echo    - File ^> Settings ^> Build ^> Gradle
echo    - Gradle JDK: Selecciona Java 17
echo    - Apply ^> OK
echo 4. File ^> Sync Project with Gradle Files
echo 5. Build ^> Generate Signed Bundle / APK
echo.
echo 🎯 JAVA 17 ES MAS ESTABLE CON GRADLE 8.x
echo.
echo ⚡ ALTERNATIVA RAPIDA:
echo Si tienes IntelliJ IDEA Ultimate, abre el proyecto ahi.
echo IntelliJ maneja mejor las incompatibilidades de Java/Gradle.
echo.
pause