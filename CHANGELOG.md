# Changelog

## v1.2.1 (versionCode 8)

### Técnico
- OCR de recibos vía Google Play Services: el modelo ya no va dentro de la app y lo descarga Play Services al instalar. La descarga baja unos 12 MB (AAB de 25,7 MB a 7,5 MB). El OCR funciona sin internet después de esa descarga; en teléfonos sin Play Services la foto se guarda pero no se detectan datos

## v1.2.0 (versionCode 7)

### Nuevas funciones
- Categorías con colores para organizar suscripciones y garantías
- Período de prueba gratuita con avisos 2 días antes y el mismo día en que termina
- Modo oscuro profesional, con selector de tema (sistema, claro, oscuro)
- Costo anual proyectado en el Dashboard
- Pantalla de Reportes con gráficas de gasto
- OCR de recibos: detecta nombre, precio y fecha desde la foto (sin internet)
- Detección de ítems duplicados al crear
- Protección opcional de backups con contraseña (cifrado AES-256), con confirmación al crear y solicitud de contraseña al restaurar

### Notificaciones corregidas
- Los recordatorios cuya hora ya pasó (p. ej. un cobro de mañana registrado hoy) se muestran de inmediato en lugar de descartarse
- Los recordatorios llegan a las 9:00 hora local, no a la hora en que se eligió la fecha
- Las suscripciones con fecha de cobro vencida avanzan solas al siguiente ciclo y siguen avisando
- Los recordatorios se restauran tras reiniciar el teléfono (el receptor de arranque no se ejecutaba)
- Revisión diaria en segundo plano como red de seguridad si el sistema descarta alarmas
- Los cambios en la configuración de notificaciones se aplican a los ítems existentes
- Textos relativos: "hoy", "mañana" o "en N días"

### Mejoras de UX
- Errores de validación bajo cada campo mientras se escribe; el diálogo de errores solo aparece al guardar (antes saltaba con cada tecla)
- La barra superior ya no queda tapada por la barra de estado en Android 15+, y el formulario queda visible sobre el teclado
- El icono de guardar del formulario ahora es visible (era blanco sobre fondo claro)

### Validación
- Nombre: máximo 60 caracteres; precio: acepta coma o punto decimal, máximo 2 decimales
- Fechas comparadas por día; la fecha de compra de una garantía no puede ser futura
- Prueba gratuita: fecha obligatoria, no anterior al inicio ni ya vencida; se desactiva al cambiar a garantía
- Categoría personalizada: máximo 30 caracteres

### Técnico
- Orientada a Android 16 (API 36), requisito de Google Play
- AGP 8.10.1, Gradle 8.11.1, Kotlin 2.1.21, KSP 2.1.21-2.0.2, Room 2.7.2 (sin cambios en el esquema de la base de datos)

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
- Soporte de cifrado AES-256 opcional para backups (solo en el código; la interfaz se agregó en v1.2.0)
- Detección automática de backups cifrados al restaurar

### Técnico
- Separación de rangos de IDs de notificación para evitar colisiones
- Estimación de tamaño de BD reemplazada por tamaño real del archivo
- Strings hardcodeados reemplazados por recursos (preparado para localización)
- Mensajes de mantenimiento desacoplados de la capa de datos

## v1.0.1 (versionCode 5)
- Versión inicial publicada
