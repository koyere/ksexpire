# PLANNING - KS Expire
**App Nativa Android para Control de Suscripciones y Vencimientos**

---

## 📋 Resumen del Proyecto

**Nombre:** KS Expire  
**Objetivo:** App simple, offline y gratuita para controlar suscripciones y vencimientos  
**Stack:** Android Nativo + Kotlin + Material Design 3 + Room + CameraX  
**Filosofía:** Sin backend, sin cuentas, sin nube - Todo local y privado  

---

## 🎯 Características Principales

### ✅ Incluye en MVP
- Registro manual de ítems (suscripciones y garantías/recibos)
- Foto del recibo (comprimida, local)
- Notificaciones locales configurables
- Buscador rápido por nombre
- Exportar/importar backup (.zip)
- Dashboard con gasto mensual fijo
- Pantalla "Sobre el Desarrollador" con links a:
  - Web: https://www.koyeresolutions.com/
  - LinkedIn: https://www.linkedin.com/in/eduardo-escobar-38a888161/
  - GitHub: https://github.com/koyere
  - Email: info@koyeresolutions.com

### ❌ NO Incluye (Futuro)
- OCR automático
- IA
- Gráficos avanzados
- Sincronización en la nube
- Cuentas de usuario

---

## 🏗️ Arquitectura Técnica

### Stack Definido
- **Lenguaje:** Kotlin
- **UI:** Android Nativo + Material Design 3
- **Arquitectura:** MVVM
- **Base de Datos:** Room (SQLite)
- **Cámara:** CameraX
- **Notificaciones:** AlarmManager + WorkManager
- **Backup:** ZIP con ACTION_CREATE_DOCUMENT

### Estructura de Base de Datos
**Tabla única:** `items`
```sql
- id (PK)
- type (0 = Garantía, 1 = Suscripción)
- name (obligatorio)
- price (opcional)
- purchaseDate (obligatorio)
- expiryDate (obligatorio)
- billingFrequency (para suscripciones)
- imagePath (ruta local)
- notificationsConfig (JSON)
- isActive (boolean)
```

---

## 📱 Diseño de Pantallas

### Dashboard (Pantalla Principal)
- **Dato destacado:** Gasto mensual fijo (suma suscripciones activas)
- **Dos secciones:**
  1. **Suscripciones** (nombre, precio, frecuencia, próximo cobro, estado)
  2. **Garantías/Recibos** (nombre, fechas, miniatura, barra progreso)

### Pantalla Crear/Editar Ítem
1. Seleccionar tipo (Suscripción o Garantía/Recibo)
2. Ingreso manual de datos
3. Tomar foto del recibo
4. Guardar

### Buscador
- Búsqueda por nombre
- Resultado inmediato
- Acceso rápido a foto del recibo

### Pantalla "Sobre el Desarrollador"
- Logo/foto personal
- Texto: "Desarrollado con ❤️ por Koyere Solutions"
- Botones de acción con todos los links especificados

---

## 🚀 Plan de Desarrollo - 8 Fases

### FASE 0 - Preparación del Proyecto
**Duración estimada:** 1 día

#### Tareas:
1. **Crear proyecto Android Studio**
   - Nombre: KS Expire
   - Package: `com.koyeresolutions.ksexpire`
   - Kotlin + Material Design 3
   - API mínima: 24 (Android 7.0)

2. **Configurar dependencias iniciales**
   ```gradle
   // Room
   implementation "androidx.room:room-runtime:$room_version"
   implementation "androidx.room:room-ktx:$room_version"
   kapt "androidx.room:room-compiler:$room_version"
   
   // CameraX
   implementation "androidx.camera:camera-camera2:$camerax_version"
   implementation "androidx.camera:camera-lifecycle:$camerax_version"
   implementation "androidx.camera:camera-view:$camerax_version"
   
   // WorkManager
   implementation "androidx.work:work-runtime-ktx:$work_version"
   
   // Material Design 3
   implementation "com.google.android.material:material:$material_version"
   
   // Google Play In-App Review API
   implementation "com.google.android.play:review:$review_version"
   implementation "com.google.android.play:review-ktx:$review_version"
   ```

3. **Configurar permisos en AndroidManifest.xml**
   ```xml
   <uses-permission android:name="android.permission.CAMERA" />
   <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
   <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
   ```
   
   **⚠️ IMPORTANTE - Permisos de Notificaciones:**
   - Google Play es estricto con `SCHEDULE_EXACT_ALARM`
   - Preparar justificación para formulario de Play Store (app de calendario/recordatorios)
   - Considerar `USE_EXACT_ALARM` como alternativa si no se requiere precisión de segundo

4. **Estructura de carpetas**
   ```
   app/src/main/java/com/koyeresolutions/ksexpire/
   ├── data/
   │   ├── database/
   │   ├── entities/
   │   └── repository/
   ├── ui/
   │   ├── dashboard/
   │   ├── create/
   │   ├── search/
   │   └── about/
   ├── utils/
   └── MainActivity.kt
   ```

#### Entregables:
- ✅ Proyecto Android configurado
- ✅ Dependencias agregadas (Room, CameraX, WorkManager, In-App Review)
- ✅ Estructura de carpetas creada
- ✅ Permisos configurados (CAMERA, SCHEDULE_EXACT_ALARM, POST_NOTIFICATIONS)
- ✅ AndroidManifest.xml con configuraciones de seguridad
- ✅ Material Design 3 con temas claro/oscuro
- ✅ Navegación Bottom Navigation configurada
- ✅ FileProvider y backup rules implementados
- ✅ Utilidades básicas (FileUtils, Constants) creadas
- ✅ Application class con inicialización de componentes

**ESTADO: ✅ COMPLETADA**

---

### FASE 1 - Modelo de Datos y Base de Datos
**Duración estimada:** 2 días

#### Tareas:
1. **Crear entidad Item**
   ```kotlin
   @Entity(tableName = "items")
   data class Item(
       @PrimaryKey(autoGenerate = true)
       val id: Long = 0,
       val type: Int, // 0 = Garantía, 1 = Suscripción
       val name: String,
       val price: Double? = null,
       val purchaseDate: Long,
       val expiryDate: Long,
       val billingFrequency: String? = null,
       val imagePath: String? = null,
       val notificationsConfig: String? = null,
       val isActive: Boolean = true
   )
   ```

2. **Crear DAO (Data Access Object)**
   ```kotlin
   @Dao
   interface ItemDao {
       @Query("SELECT * FROM items WHERE isActive = 1")
       fun getAllActiveItems(): Flow<List<Item>>
       
       @Query("SELECT * FROM items WHERE type = :type AND isActive = 1")
       fun getItemsByType(type: Int): Flow<List<Item>>
       
       @Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%'")
       fun searchItems(query: String): Flow<List<Item>>
       
       @Insert
       suspend fun insertItem(item: Item): Long
       
       @Update
       suspend fun updateItem(item: Item)
       
       @Delete
       suspend fun deleteItem(item: Item)
   }
   ```

3. **Crear RoomDatabase**
   ```kotlin
   @Database(
       entities = [Item::class],
       version = 1,
       exportSchema = false
   )
   @TypeConverters(Converters::class)
   abstract class AppDatabase : RoomDatabase() {
       abstract fun itemDao(): ItemDao
   }
   ```

4. **Crear Repository**
   ```kotlin
   class ItemRepository(private val itemDao: ItemDao) {
       fun getAllActiveItems() = itemDao.getAllActiveItems()
       fun getSubscriptions() = itemDao.getItemsByType(1)
       fun getWarranties() = itemDao.getItemsByType(0)
       fun searchItems(query: String) = itemDao.searchItems(query)
       
       suspend fun insertItem(item: Item) = itemDao.insertItem(item)
       suspend fun updateItem(item: Item) = itemDao.updateItem(item)
       suspend fun deleteItem(item: Item) = itemDao.deleteItem(item)
   }
   ```

#### Entregables:
- ✅ **COMPLETADO** - Entidad Item creada con validaciones y lógica de negocio
- ✅ **COMPLETADO** - DAO implementado con 25+ consultas optimizadas
- ✅ **COMPLETADO** - Database configurada con índices y optimizaciones WAL
- ✅ **COMPLETADO** - Repository implementado con lógica de normalización
- ✅ **COMPLETADO** - Utilidades DateUtils y CurrencyUtils implementadas
- ✅ **COMPLETADO** - Pruebas unitarias con cobertura de casos críticos

**✅ FASE 1 COMPLETADA** - Base de datos 100% funcional con todas las características críticas del planning implementadas.

---

### FASE 2 - CRUD Básico y Dashboard
**Duración estimada:** 3 días

#### Tareas:
1. **Crear MainActivity con Navigation**
   - Bottom Navigation con 3 tabs: Dashboard, Buscar, Acerca de
   - Fragment container para navegación

2. **Implementar DashboardFragment**
   ```kotlin
   class DashboardFragment : Fragment() {
       // Mostrar gasto mensual fijo
       // RecyclerView para suscripciones
       // RecyclerView para garantías/recibos
   }
   ```

3. **Crear adaptadores para RecyclerView**
   - `SubscriptionAdapter`: mostrar nombre, precio, frecuencia, próximo cobro
   - `WarrantyAdapter`: mostrar nombre, fechas, barra de progreso

4. **Implementar CreateEditItemActivity**
   - Formulario para ingresar datos manualmente
   - Validaciones (nombre y fechas obligatorias)
   - Selector de tipo (Suscripción/Garantía)

5. **ViewModel para Dashboard**
   ```kotlin
   class DashboardViewModel(private val repository: ItemRepository) : ViewModel() {
       val subscriptions = repository.getSubscriptions()
       val warranties = repository.getWarranties()
       
       fun calculateMonthlyExpense(): LiveData<Double> {
           // IMPORTANTE: Normalizar gastos a mensual
           // Si frecuencia = Mensual ➝ Sumar price
           // Si frecuencia = Anual ➝ Sumar price / 12
           // Esto muestra el "Burn Rate" real mensual del usuario
       }
   }
   ```

#### Entregables:
- ✅ **COMPLETADO** - Dashboard funcional con listas y estados vacíos
- ✅ **COMPLETADO** - Formulario crear/editar ítem con validaciones completas
- ✅ **COMPLETADO** - Navegación entre pantallas con Material Design 3
- ✅ **COMPLETADO** - Cálculo de gasto mensual **normalizado** (anual/12, semanal*4.33)
- ✅ **COMPLETADO** - Detección automática de moneda local implementada
- ✅ **COMPLETADO** - Validaciones robustas con mensajes de error
- ✅ **COMPLETADO** - Adaptadores con animaciones y estados de vigencia
- ✅ **COMPLETADO** - ViewModels con arquitectura MVVM profesional

**✅ FASE 2 COMPLETADA** - Dashboard y CRUD completamente funcionales con UI profesional.

---

### FASE 3 - Integración de Cámara
**Duración estimada:** 2 días

#### Tareas:
1. **Implementar CameraActivity**
   ```kotlin
   class CameraActivity : AppCompatActivity() {
       private lateinit var imageCapture: ImageCapture
       
       private fun takePhoto() {
           // Capturar foto
           // Comprimir JPEG (70-80%)
           // Guardar en filesDir/receipts/
       }
   }
   ```

2. **Crear utilidad para manejo de imágenes**
   ```kotlin
   object ImageUtils {
       fun compressAndSaveImage(bitmap: Bitmap, path: String): String
       fun loadImageFromPath(path: String): Bitmap?
       fun deleteImageFile(path: String): Boolean
   }
   ```

3. **Integrar cámara en CreateEditItemActivity**
   - Botón "Tomar foto del recibo"
   - Preview de imagen capturada
   - Opción para retomar foto

4. **Configurar almacenamiento privado**
   - Crear carpeta `filesDir/receipts/`
   - Generar nombres únicos para archivos
   - **IMPORTANTE:** Guardar solo nombre de archivo en BD (no ruta absoluta)
   - Construir ruta completa dinámicamente: `File(context.filesDir, "receipts/" + item.imagePath)`
   - Limpiar imágenes huérfanas

#### Entregables:
- ✅ **COMPLETADO** - Cámara funcional con CameraX y UI profesional
- ✅ **COMPLETADO** - Compresión automática de imágenes (75% calidad)
- ✅ **COMPLETADO** - Almacenamiento en carpeta privada con **rutas relativas**
- ✅ **COMPLETADO** - Preview de imágenes optimizado con miniaturas
- ✅ **COMPLETADO** - Corrección automática de orientación EXIF
- ✅ **COMPLETADO** - Gestión de archivos temporales y limpieza
- ✅ **COMPLETADO** - Visor de imagen completa con zoom (PhotoView)
- ✅ **COMPLETADO** - Integración completa con formulario crear/editar

**✅ FASE 3 COMPLETADA** - Cámara completamente integrada con procesamiento profesional de imágenes.

---

### FASE 4 - Sistema de Notificaciones
**Duración estimada:** 2 días

#### Tareas:
1. **Crear NotificationManager personalizado**
   ```kotlin
   class NotificationManager(private val context: Context) {
       fun scheduleExpiryNotification(item: Item, daysBefore: Int)
       fun scheduleBillingNotification(item: Item, daysBefore: Int)
       fun cancelNotification(itemId: Long)
   }
   ```

2. **Implementar AlarmManager para fechas exactas**
   ```kotlin
   class AlarmScheduler {
       fun scheduleAlarm(itemId: Long, triggerTime: Long)
       fun cancelAlarm(itemId: Long)
   }
   ```

3. **Crear WorkManager para reprogramación**
   ```kotlin
   class NotificationWorker : Worker() {
       override fun doWork(): Result {
           // Reprogramar alarmas después de reinicio
           return Result.success()
       }
   }
   ```

4. **Configurar tipos de notificaciones**
   - Suscripciones: 1 día antes del cobro (configurable)
   - Garantías: 30 días y 7 días antes (opcional)

5. **Pantalla de configuración de notificaciones**
   - Activar/desactivar por tipo
   - Configurar días de anticipación
   - **Configuración de moneda:** Selector de símbolo de moneda preferido
   - Usar `NumberFormat.getCurrencyInstance()` para detección automática local

#### Entregables:
- ✅ **COMPLETADO** - Notificaciones locales con AlarmManager y WorkManager
- ✅ **COMPLETADO** - Programación automática al crear/editar ítems
- ✅ **COMPLETADO** - Reprogramación tras reinicio con BootReceiver
- ✅ **COMPLETADO** - Configuración personalizable de moneda y notificaciones
- ✅ **COMPLETADO** - Cancelación automática al eliminar ítems
- ✅ **COMPLETADO** - NotificationService integrado con Repository
- ✅ **COMPLETADO** - Pantalla de configuración completa
- ✅ **COMPLETADO** - Manejo de permisos SCHEDULE_EXACT_ALARM

**✅ FASE 4 COMPLETADA** - Sistema de notificaciones completamente funcional e integrado.

---

### FASE 5 - Buscador
**Duración estimada:** 1 día

#### Tareas:
1. **Implementar SearchFragment**
   ```kotlin
   class SearchFragment : Fragment() {
       private lateinit var searchView: SearchView
       private lateinit var resultsAdapter: SearchResultsAdapter
       
       private fun performSearch(query: String) {
           // Búsqueda en tiempo real
       }
   }
   ```

2. **Crear SearchResultsAdapter**
   - Mostrar nombre del ítem
   - Tipo (suscripción/garantía)
   - Acceso rápido a foto del recibo
   - Navegación al detalle

3. **Optimizar búsqueda**
   - Debounce para evitar búsquedas excesivas
   - Highlight de términos encontrados
   - Ordenar por relevancia

#### Entregables:
- ✅ **COMPLETADO** - Buscador funcional en tiempo real con debounce
- ✅ **COMPLETADO** - Resultados ordenados por relevancia y tipo
- ✅ **COMPLETADO** - Acceso rápido a fotos de recibos con zoom
- ✅ **COMPLETADO** - Navegación fluida desde resultados a edición
- ✅ **COMPLETADO** - Filtros por tipo (Todos, Suscripciones, Garantías)
- ✅ **COMPLETADO** - Estados vacío y sin resultados con UX clara
- ✅ **COMPLETADO** - Estadísticas de búsqueda en tiempo real
- ✅ **COMPLETADO** - Acciones rápidas (activar/desactivar, eliminar)

**✅ FASE 5 COMPLETADA** - Buscador completamente funcional con filtros avanzados y UX profesional.

---

### FASE 6 - Backup y Restauración
**Duración estimada:** 2 días

#### Tareas:
1. **Implementar BackupManager**
   ```kotlin
   class BackupManager(private val context: Context) {
       suspend fun createBackup(): Uri? {
           // Crear archivo ZIP
           // Incluir base de datos
           // Incluir todas las imágenes
           // Usar ACTION_CREATE_DOCUMENT
       }
       
       suspend fun restoreBackup(uri: Uri): Boolean {
           // Validar archivo ZIP
           // Restaurar base de datos
           // Restaurar imágenes
       }
   }
   ```

2. **Crear pantalla de Backup/Restore**
   - Botón "Exportar copia de seguridad"
   - Botón "Importar copia de seguridad"
   - Indicador de progreso
   - Mensajes de confirmación

3. **Validaciones de importación**
   - Verificar integridad del ZIP
   - Validar estructura de base de datos
   - Confirmar antes de sobrescribir datos

4. **Manejo de errores**
   - Permisos de almacenamiento
   - Archivos corruptos
   - Espacio insuficiente

#### Entregables:
- ✅ **COMPLETADO** - BackupManager con exportación/importación ZIP completa
- ✅ **COMPLETADO** - BackupActivity con UI profesional y progreso
- ✅ **COMPLETADO** - BackupViewModel con validaciones y manejo de errores
- ✅ **COMPLETADO** - Integración con navegación principal (menú Dashboard)
- ✅ **COMPLETADO** - Iconos profesionales (ic_backup, ic_restore, ic_storage, ic_check_circle)
- ✅ **COMPLETADO** - Validación de archivos ZIP y compatibilidad de versiones
- ✅ **COMPLETADO** - Preservación de integridad de datos con metadata
- ✅ **COMPLETADO** - Manejo robusto de errores y estados de carga
- ✅ **COMPLETADO** - Registro en AndroidManifest.xml

**✅ FASE 6 COMPLETADA** - Sistema de backup/restore completamente funcional e integrado.

---

### FASE 7 - Pantalla "Sobre el Desarrollador"
**Duración estimada:** 1 día

#### Tareas:
1. **Crear AboutFragment**
   ```kotlin
   class AboutFragment : Fragment() {
       private fun setupDeveloperInfo() {
           // Logo/foto personal
           // Texto motivacional
           // Botones de acción
       }
   }
   ```

2. **Implementar botones de acción**
   - 🌐 Web: https://www.koyeresolutions.com/
   - 💼 LinkedIn: https://www.linkedin.com/in/eduardo-escobar-38a888161/
   - 🧑‍💻 GitHub: https://github.com/koyere
   - ⭐ Calificar App (Google Play)
   - 📧 Contacto: info@koyeresolutions.com (mailto:)

3. **Diseño atractivo**
   - Material Design 3
   - Animaciones sutiles
   - Cards para cada sección
   - Iconos apropiados

4. **Funcionalidad de enlaces**
   - Abrir URLs en navegador
   - Abrir LinkedIn/GitHub en apps nativas si están instaladas
   - Crear email con asunto predefinido
   - **Implementar Google Play In-App Review API** para calificaciones sin salir de la app
   - Disparar popup de review después del 3er ítem creado (momento de satisfacción)

#### Entregables:
- ✅ **COMPLETADO** - AboutFragment con diseño Material Design 3 profesional
- ✅ **COMPLETADO** - AboutViewModel con Google Play In-App Review API
- ✅ **COMPLETADO** - PreferencesManager para configuraciones y contadores
- ✅ **COMPLETADO** - Todos los enlaces del desarrollador funcionando:
  - 🌐 Sitio web: koyeresolutions.com
  - 💼 LinkedIn con app nativa o navegador
  - 🧑‍💻 GitHub con app nativa o navegador
  - ⭐ Calificar app con In-App Review API
  - 📧 Email con asunto y cuerpo predefinidos
- ✅ **COMPLETADO** - Trigger automático de review después del 3er ítem creado
- ✅ **COMPLETADO** - Información técnica de la app con detalles completos
- ✅ **COMPLETADO** - Iconos profesionales (web, linkedin, github, star, email, arrow)
- ✅ **COMPLETADO** - Integración completa con navegación principal
- ✅ **COMPLETADO** - Manejo de errores y fallbacks a Play Store

**✅ FASE 7 COMPLETADA** - Pantalla "Sobre el Desarrollador" completamente funcional con In-App Review API.

---

### FASE 8 - Preparación para Publicación
**Duración estimada:** 3 días

#### Tareas:
1. **Crear Política de Privacidad**
   - Documento público (GitHub Pages o sitio estático)
   - Explicar uso de cámara únicamente para recibos
   - Confirmar almacenamiento local únicamente
   - Sin recopilación ni transmisión de datos
   - URL requerida para Google Play

2. **Diseñar assets para Play Store**
   - **Icono de app** (512x512, adaptativo)
   - **Feature Graphic** (1024x500)
   - **Screenshots** (mínimo 2, máximo 8):
     - Dashboard con datos de ejemplo
     - Lista de suscripciones
     - Lista de garantías con barras de progreso
     - Detalle con foto de recibo
     - Pantalla de búsqueda
     - Pantalla "Sobre el Desarrollador"

3. **Configurar app para release**
   - Generar keystore para firma
   - Configurar ProGuard/R8
   - Optimizar APK size
   - Versioning (versionCode: 1, versionName: "1.0.0")

4. **Preparar descripción para Play Store**
   ```
   Título: KS Expire - Control de Suscripciones
   
   Descripción corta:
   Controla tus suscripciones y recibos. Todo offline. Sin cuentas.
   
   Descripción larga:
   ¿Cansado de suscripciones, anuncios y apps que espían?
   KS Expire es diferente.
   Una herramienta creada por un desarrollador independiente que cree en la privacidad.
   Sin internet. Sin anuncios. Sin trucos. Solo utilidad.
   
   Características:
   • Control de suscripciones y fechas de vencimiento
   • Fotos de recibos almacenadas localmente
   • Notificaciones antes de vencimientos
   • Búsqueda rápida
   • Backup completo sin nube
   • Soporte multi-moneda automático
   • 100% privado - sin cuentas ni servidores
   ```

5. **Preparar documentación para permisos especiales**
   - Justificación para `SCHEDULE_EXACT_ALARM` en formulario de Google Play
   - Explicar uso legítimo para recordatorios de vencimientos
   - Documentar que es una app de calendario/recordatorios

5. **Testing final**
   - Pruebas en diferentes dispositivos
   - Verificar notificaciones
   - Probar backup/restore
   - Validar todos los flujos

#### Entregables:
- ✅ Política de Privacidad publicada
- ✅ Assets de Play Store completos
- ✅ APK firmado y optimizado
- ✅ Descripción de tienda preparada
- ✅ **Documentación de permisos especiales** para Google Play
- ✅ Testing completo realizado
- ✅ App lista para publicación

---

## 📊 Cronograma Total

| Fase | Descripción | Duración | Días Acumulados |
|------|-------------|----------|-----------------|
| 0 | Preparación del Proyecto | 1 día | 1 |
| 1 | Modelo de Datos | 2 días | 3 | ✅ **COMPLETADO** |
| 2 | CRUD Básico y Dashboard | 3 días | 6 | ✅ **COMPLETADO** |
| 3 | Integración de Cámara | 2 días | 8 | ✅ **COMPLETADO** |
| 4 | Sistema de Notificaciones | 2 días | 10 | ✅ **COMPLETADO** |
| 5 | Buscador | 1 día | 11 | ✅ **COMPLETADO** |
| 6 | Backup y Restauración | 2 días | 13 |
| 7 | Sobre el Desarrollador | 1 día | 14 |
| 8 | Preparación para Publicación | 3 días | 17 |

**Duración total estimada: 17 días de desarrollo**

## 📈 Progreso Actual

**✅ FASES COMPLETADAS: 7/8**
- ✅ **FASE 0:** Preparación del Proyecto (1 día)
- ✅ **FASE 1:** Modelo de Datos y Base de Datos (2 días) 
- ✅ **FASE 2:** CRUD Básico y Dashboard (3 días)
- ✅ **FASE 3:** Integración de Cámara (2 días)
- ✅ **FASE 4:** Sistema de Notificaciones (2 días)
- ✅ **FASE 5:** Buscador (1 día)
- ✅ **FASE 6:** Backup y Restauración (2 días)
- ✅ **FASE 7:** Sobre el Desarrollador (1 día)

**🚀 EN PROGRESO:**
- ⏸️ **FASE 8:** Preparación para Publicación (3 días)

**⏳ PENDIENTES:**
- ⏸️ FASE 8: Preparación para Publicación (3 días)

**📊 Progreso: 82% completado (14/17 días)**

## 🌐 Repositorio Público

**GitHub**: https://github.com/koyere/ksexpire

### ✅ Configuración Completada:
- 🔒 **Licencia MIT**: Código abierto auditable
- 📄 **PRIVACY.md**: Política de privacidad completa (GitHub Pages ready)
- 📖 **README.md**: Documentación profesional con badges
- 🛡️ **.gitignore**: Configuración segura para Android
- 🚀 **Marketing de Confianza**: "App de Código Abierto. Auditable en GitHub. Cero rastreadores ocultos."
- 💼 **Portafolio Vivo**: Proyecto activo con arquitectura limpia (MVVM, Room, Clean Code)

---

## ⚡ Consideraciones Técnicas Críticas

### 1. Lógica del "Gasto Mensual" (Normalización)
**Problema:** Si un usuario agrega una suscripción anual de $120 (ej. Amazon Prime), sumar directamente el precio distorsiona la realidad financiera mensual.

**Solución:** En `DashboardViewModel`, normalizar el cálculo:
```kotlin
fun calculateMonthlyExpense(subscriptions: List<Item>): Double {
    return subscriptions.sumOf { subscription ->
        when (subscription.billingFrequency) {
            "MONTHLY" -> subscription.price ?: 0.0
            "ANNUAL" -> (subscription.price ?: 0.0) / 12.0
            "WEEKLY" -> (subscription.price ?: 0.0) * 4.33 // promedio semanal a mensual
            else -> 0.0
        }
    }
}
```
**Resultado:** El usuario ve su "Burn Rate" (cuota de quema) real mensual.

### 2. Rutas de Imágenes: Relativas vs. Absolutas
**Problema:** Guardar rutas absolutas rompe los enlaces al migrar dispositivos o actualizar la app.

**Solución:** 
- Guardar solo el nombre del archivo en BD: `img_20241020_123456.jpg`
- Construir ruta completa dinámicamente: `File(context.filesDir, "receipts/" + item.imagePath)`
- Hace el Backup/Restore robusto entre dispositivos

### 3. Moneda y Localización
**Problema:** La app puede ser descargada en España (€), UK (£), México ($), etc.

**Solución:**
```kotlin
// Detección automática
val currencyFormat = NumberFormat.getCurrencyInstance()

// O configuración manual en ajustes
class CurrencyPreferences {
    fun getSelectedCurrency(): String // "$", "€", "£", etc.
}
```

### 4. In-App Review API (Vital para Reputación)
**Problema:** Enviar al usuario a Play Store corta el flujo y reduce conversiones.

**Solución:** Implementar Google Play In-App Review API
```kotlin
class ReviewManager {
    fun requestReviewIfEligible(itemsCreated: Int) {
        if (itemsCreated == 3) { // Momento de satisfacción
            showInAppReview()
        }
    }
}
```
**Beneficio:** Tasa de conversión de calificaciones sube drásticamente.

### 5. Permisos de Notificaciones en Android 13/14
**Advertencia:** Google Play es muy estricto con `SCHEDULE_EXACT_ALARM`.

**Estrategia:**
- Justificar en formulario de Play Store (app de calendario/recordatorios está permitido)
- Alternativa: `USE_EXACT_ALARM` (no requiere permiso especial para apps de calendario)
- Preparar documentación que explique por qué necesitas precisión exacta

---

## 🎨 Consideraciones de Diseño

### Animaciones Permitidas (Funcionales)
- Transiciones de pantalla con MaterialSharedAxis
- Ripple effects nativos en botones
- Micro-animaciones al guardar/eliminar
- LottieFiles ligeros solo para estados vacíos

### Colores y Tema
- Material Design 3 con tema dinámico
- Soporte para modo oscuro
- Colores para barras de progreso:
  - Verde: reciente
  - Amarillo: media vigencia
  - Rojo: por vencer

### Tipografía
- Material Design 3 typography scale
- Énfasis en legibilidad
- Jerarquía clara de información

---

## 🔒 Consideraciones de Seguridad y Privacidad

### Almacenamiento
- Base de datos en almacenamiento privado de la app
- Imágenes en `filesDir/receipts/` (no accesibles por otras apps)
- Sin acceso a almacenamiento externo público

### Permisos Mínimos
- CAMERA: solo para tomar fotos de recibos
- SCHEDULE_EXACT_ALARM: para notificaciones precisas
- POST_NOTIFICATIONS: para mostrar alertas

### Sin Red
- Sin permisos de internet
- Sin servicios en la nube
- Sin analytics ni tracking

---

## 📝 Notas Importantes

1. **Mensaje clave al usuario:** "La foto es respaldo. Los datos los controlas tú."

2. **Filosofía de desarrollo:** Mantener la simplicidad y privacidad en cada decisión

3. **Monetización:** A través de visibilidad y reputación, no dinero

4. **Escalabilidad:** Arquitectura preparada para futuras funcionalidades sin comprometer la simplicidad actual

5. **Testing:** Probar en dispositivos con diferentes versiones de Android (mínimo API 24)

---

## ✅ Criterios de Éxito del MVP

- [ ] App funciona completamente offline
- [ ] Registro manual de suscripciones y garantías
- [ ] Cámara integrada con compresión automática
- [ ] Notificaciones locales configurables
- [ ] Buscador rápido y efectivo
- [ ] Backup/restore funcional
- [ ] Dashboard con gasto mensual calculado
- [ ] Pantalla "Sobre el Desarrollador" con todos los enlaces
- [ ] Política de Privacidad publicada
- [ ] Assets de Play Store completos
- [ ] App publicada en Google Play Store

---

**Estado del Planning:** ✅ Completo y listo para ejecución

**Próximo paso:** Iniciar FASE 0 - Preparación del Proyecto