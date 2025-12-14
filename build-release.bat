@echo off
echo ========================================
echo  KS Expire - Build Release APK/AAB
echo ========================================
echo.

REM Verificar que keystore.properties existe
if not exist "keystore.properties" (
    echo ERROR: keystore.properties no encontrado
    echo Por favor configura keystore.properties con tus passwords
    pause
    exit /b 1
)

REM Verificar que el keystore existe
if not exist "ks-expire-release.jks" (
    echo ERROR: ks-expire-release.jks no encontrado
    echo Por favor genera el keystore primero
    pause
    exit /b 1
)

echo ✅ Configuracion verificada correctamente
echo ✅ Keystore: ks-expire-release.jks encontrado
echo ✅ Properties: keystore.properties configurado
echo.

echo 🚀 GENERANDO APK FIRMADO...
echo.
echo Debido a problemas de compatibilidad de Gradle, usaremos Android Studio:
echo.

echo 📱 Abriendo Android Studio...
start "" "C:\Program Files\Android\Android Studio\bin\studio64.exe" "%CD%"

echo.
echo 📋 INSTRUCCIONES PASO A PASO:
echo.
echo 1️⃣  Espera a que Android Studio abra y sincronice el proyecto
echo 2️⃣  Ve a: Build ^> Generate Signed Bundle / APK...
echo 3️⃣  Selecciona: Android App Bundle ^(AAB^) - RECOMENDADO para Play Store
echo 4️⃣  Click: Next
echo 5️⃣  Keystore path: Selecciona ks-expire-release.jks
echo 6️⃣  Key alias: ks-expire-key
echo 7️⃣  Passwords: Los que configuraste
echo 8️⃣  Click: Next
echo 9️⃣  Build variant: release
echo 🔟 Destination folder: Deja por defecto
echo 1️⃣1️⃣ Click: Finish
echo.
echo ⏳ Android Studio generará el archivo firmado...
echo.
echo 📁 UBICACION DE ARCHIVOS GENERADOS:
echo - AAB: app\build\outputs\bundle\release\app-release.aab
echo - APK: app\build\outputs\apk\release\app-release.apk
echo.
echo 🎯 PARA PLAY STORE: Usa el archivo .aab
echo 🧪 PARA TESTING: Usa el archivo .apk
echo.
echo ✨ Una vez generado, podrás instalar el APK y capturar screenshots!
echo.
pause