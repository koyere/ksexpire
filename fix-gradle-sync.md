# Solución Rápida para Gradle Sync en Android Studio

## 🔧 Pasos para Solucionar el Error de Gradle:

### 1. En Android Studio:
1. **File** → **Invalidate Caches and Restart** → **Invalidate and Restart**
2. Espera a que Android Studio se reinicie

### 2. Después del reinicio:
1. **File** → **Sync Project with Gradle Files**
2. Si sigue fallando, continúa con el paso 3

### 3. Limpiar y reconstruir:
1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**

### 4. Si aún hay problemas:
1. Ve a **File** → **Settings** → **Build, Execution, Deployment** → **Gradle**
2. Cambia **Gradle JDK** a **Project SDK** o **Java 11**
3. Click **Apply** y **OK**
4. **File** → **Sync Project with Gradle Files**

### 5. Alternativa - Usar Gradle Wrapper específico:
1. En la terminal de Android Studio (View → Tool Windows → Terminal):
```bash
./gradlew clean
./gradlew assembleRelease
```

### 6. Si nada funciona - Método Manual:
1. **Build** → **Select Build Variant**
2. Cambia de **debug** a **release**
3. **Build** → **Make Project**
4. Ve a **app/build/outputs/apk/release/** para encontrar el APK

## 🎯 Objetivo:
Generar **app-release.apk** en la carpeta `app/build/outputs/apk/release/`

## ⚠️ Nota Importante:
El keystore y passwords ya están configurados correctamente. El problema es solo de sincronización de Gradle.

## 🚀 Una vez que tengas el APK:
1. Instálalo en tu dispositivo
2. Agrega datos de ejemplo
3. Captura los 5 screenshots según ASSETS.md
4. ¡Listo para Play Store!