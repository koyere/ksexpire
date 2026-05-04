# Changelog

## v1.1.0 (versionCode 6)

### Notificaciones corregidas
- Solicitud de permiso POST_NOTIFICATIONS en Android 13+ (las notificaciones no se mostraban)
- Verificación de permiso SCHEDULE_EXACT_ALARM en Android 12+ con fallback a alarma inexacta
- Las preferencias de notificación del usuario ahora se respetan al programar recordatorios
- Reprogramación automática de notificaciones al abrir la app (red de seguridad)
- Uso de goAsync() en NotificationReceiver para evitar pérdida de notificaciones

### Estabilidad y rendimiento
- Colección de Flows con repeatOnLifecycle para evitar memory leaks en Dashboard y Búsqueda
- Procesamiento de imágenes de cámara movido a hilo secundario (evita ANR)
- Eliminado fallbackToDestructiveMigration de la base de datos (previene pérdida de datos en futuras actualizaciones)
- Corregida condición de carrera al reprogramar suscripciones desde NotificationReceiver
- Corregida recarga excesiva de datos en el Dashboard

### Mejoras de UX
- Estado vacío del Dashboard ahora se muestra correctamente
- Validación de formulario en tiempo real al crear/editar ítems
- Diálogo de confirmación antes de restaurar backup (advierte sobre reemplazo de datos)
- Navegación a búsqueda con filtro ya no se re-aplica al rotar pantalla

### Seguridad
- Soporte de cifrado AES-256 opcional para backups
- Detección automática de backups cifrados al restaurar

### Técnico
- Separación de rangos de IDs de notificación para evitar colisiones
- Estimación de tamaño de BD reemplazada por tamaño real del archivo
- Strings hardcodeados reemplazados por recursos (preparado para localización)
- Mensajes de mantenimiento desacoplados de la capa de datos

## v1.0.1 (versionCode 5)
- Versión inicial publicada
