# 📱 KS Expire - Control de Suscripciones y Vencimientos

[![Privacy First](https://img.shields.io/badge/Privacy-First-green.svg)](PRIVACY.md)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blue.svg)](https://kotlinlang.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Open Source](https://img.shields.io/badge/Open%20Source-❤️-red.svg)](https://github.com/koyere/ksexpire)

> **Una app simple, offline y gratuita para controlar suscripciones y vencimientos. Sin cuentas, sin servidores, sin espionaje.**

---

## 🎯 ¿Por Qué KS Expire?

### El Problema
- Las apps de finanzas te espían y venden tus datos
- Requieren cuentas, permisos excesivos y conexión constante
- Están llenas de publicidad y funciones innecesarias
- No sabes qué hacen realmente con tu información

### La Solución: KS Expire
- **100% Offline**: Sin internet, sin servidores, sin cuentas
- **100% Privada**: Tus datos nunca salen de tu dispositivo
- **100% Gratuita**: Sin anuncios, sin suscripciones, sin trucos
- **100% Auditable**: Código abierto, transparencia total

---

## ✨ Características

### 📊 Dashboard Inteligente
- Gasto mensual normalizado (anual/12, semanal*4.33)
- Vista separada de suscripciones y garantías
- Indicadores visuales de vencimientos próximos

### 📷 Fotos de Recibos
- Toma fotos de garantías y recibos
- Compresión automática para ahorrar espacio
- Almacenamiento local seguro

### 🔔 Notificaciones Inteligentes
- Recordatorios antes de cobros de suscripciones
- Alertas de vencimiento de garantías
- Configuración personalizable por tipo

### 🔍 Búsqueda Rápida
- Búsqueda en tiempo real
- Filtros por tipo (suscripciones/garantías)
- Acceso rápido a fotos de recibos

### 💾 Backup Completo
- Exporta todos tus datos a ZIP
- Incluye base de datos e imágenes
- Importa en cualquier dispositivo
- Sin dependencia de servicios en la nube

### 🌍 Multi-Moneda
- Detección automática de moneda local
- Soporte para múltiples símbolos de moneda
- Configuración manual disponible

---

## 🛡️ Privacidad y Seguridad

### Sin Rastreadores
```
❌ Google Analytics    ❌ Facebook SDK
❌ Crashlytics        ❌ Publicidad
❌ Servicios en la nube ❌ Cuentas de usuario
```

### Permisos Mínimos
- **📷 Cámara**: Solo para fotos de recibos
- **🔔 Notificaciones**: Solo para recordatorios
- **⏰ Alarmas**: Solo para fechas exactas
- **🔄 Boot**: Solo para reprogramar tras reinicio

### Código Abierto
- Todo el código es público y auditable
- Licencia MIT - libre para revisar y usar
- Sin dependencias sospechosas
- Transparencia total

---

## 📱 Screenshots

*[Screenshots se agregarán cuando la app esté en Play Store]*

---

## 🚀 Tecnología

### Stack Técnico
- **Lenguaje**: Kotlin 100%
- **UI**: Android Nativo + Material Design 3
- **Arquitectura**: MVVM + Repository Pattern
- **Base de Datos**: Room (SQLite)
- **Cámara**: CameraX
- **Notificaciones**: AlarmManager + WorkManager

### Características Técnicas
- Arquitectura limpia y escalable
- Inyección de dependencias manual
- Corrutinas para operaciones asíncronas
- LiveData y StateFlow para UI reactiva
- Backup/restore con validación de integridad

---

## 📥 Descarga

### Google Play Store
*[Link se agregará cuando esté publicada]*

### APK Directo
*[Link se agregará en releases de GitHub]*

---

## 🛠️ Desarrollo

### Requisitos
- Android Studio Arctic Fox o superior
- Kotlin 1.8+
- Android SDK 24+ (Android 7.0)
- Gradle 8.0+

### Configuración
```bash
git clone https://github.com/koyere/ksexpire.git
cd ksexpire
./gradlew build
```

### Estructura del Proyecto
```
app/src/main/java/com/koyeresolutions/ksexpire/
├── data/           # Entidades, DAOs, Repository
├── ui/             # Activities, Fragments, ViewModels
├── utils/          # Utilidades y helpers
├── notifications/  # Sistema de notificaciones
└── backup/         # Sistema de backup/restore
```

---

## 🤝 Contribuir

### ¿Cómo Ayudar?
1. **🐛 Reporta bugs**: Abre un issue con detalles
2. **💡 Sugiere mejoras**: Ideas para nuevas funciones
3. **🔍 Audita el código**: Revisa la seguridad y privacidad
4. **📖 Mejora documentación**: Ayuda con traducciones
5. **⭐ Da una estrella**: Si te gusta el proyecto

### Código de Conducta
- Respeto y profesionalismo
- Enfoque en privacidad y simplicidad
- Código limpio y bien documentado
- Testing antes de pull requests

---

## 📄 Licencia

Este proyecto está bajo la [Licencia MIT](LICENSE) - libre para usar, modificar y distribuir.

### ¿Qué Significa?
- ✅ Uso comercial permitido
- ✅ Modificación permitida
- ✅ Distribución permitida
- ✅ Uso privado permitido
- ⚠️ Sin garantía
- ⚠️ Incluir licencia y copyright

---

## 👨‍💻 Desarrollador

**Eduardo Escobar - Koyere Solutions**

- 🌐 **Web**: [koyeresolutions.com](https://www.koyeresolutions.com/)
- 💼 **LinkedIn**: [Eduardo Escobar](https://www.linkedin.com/in/eduardo-escobar-38a888161/)
- 🧑‍💻 **GitHub**: [@koyere](https://github.com/koyere)
- 📧 **Email**: info@koyeresolutions.com

---

## 🙏 Agradecimientos

- **Material Design 3**: Por los componentes de UI
- **Android Jetpack**: Por las bibliotecas robustas
- **Kotlin**: Por hacer el desarrollo Android más agradable
- **Comunidad Open Source**: Por inspirar la transparencia

---

## 📊 Estadísticas del Proyecto

- **Líneas de código**: ~5,000+
- **Tiempo de desarrollo**: 17 días
- **Arquitectura**: MVVM Clean
- **Cobertura de tests**: En desarrollo
- **Tamaño de APK**: ~8MB (estimado)

---

**¿Te gusta el proyecto? ⭐ Dale una estrella en GitHub y compártelo con otros desarrolladores que valoren la privacidad.**

---

*"La foto es respaldo. Los datos los controlas tú."* - KS Expire