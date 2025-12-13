# KS Expire

**Control simple y privado de suscripciones, gastos y vencimientos.**

---

## 1. Objetivo del Proyecto

Crear una app **simple, offline y gratuita** que permita al usuario controlar:

* Lo que **paga de forma recurrente** (suscripciones).
* Lo que **vence** (garantías, recibos, pagos únicos).

Sin cuentas. Sin nube. Sin backend. Toda la información vive **solo en el dispositivo**.

---

## 2. Propuesta de Valor

* **Privacidad total:** ningún dato sale del teléfono.
* **Orden mental:** todo lo que tiene fecha de vencimiento en un solo lugar.
* **Utilidad real:** alertas antes de pagar o perder una garantía.
* **Mantenimiento mínimo:** arquitectura local, estable y predecible.

---

## 3. Alcance del MVP (CERRADO)

### Incluye

* Registro manual de ítems.
* Foto del recibo (comprimida, local).
* Notificaciones locales.
* Buscador rápido.
* Exportar / importar copia de seguridad.

### NO incluye (por ahora)

* OCR automático.
* IA.
* Gráficos avanzados.
* Sincronización en la nube.
* Cuentas de usuario.

---

## 4. Estructura de la App

### Pantalla Inicial (Dashboard)

* Dato único destacado:

  * **Gasto mensual fijo** (suma de suscripciones activas).

* Dos secciones verticales:

  * **Suscripciones**
  * **Garantías / Recibos**

---

### Sección: Suscripciones

Cada ítem muestra:

* Nombre
* Precio
* Frecuencia (mensual / anual)
* Próximo cobro
* Estado (activa / pausada)

Notificación:

* 1 día antes del cobro (configurable).

---

### Sección: Garantías / Recibos

Cada ítem muestra:

* Nombre del producto
* Fecha de compra
* Fecha de vencimiento
* Miniatura del recibo
* Barra de progreso de vigencia

  * Verde: reciente
  * Amarillo: media
  * Rojo: por vencer

Notificaciones:

* 30 días antes (opcional)
* 7 días antes (opcional)

---

## 5. Flujo de Creación de Ítem

1. Seleccionar tipo:

   * Suscripción
   * Garantía / Recibo
2. Ingreso **manual** de datos:

   * Nombre (obligatorio)
   * Fechas (obligatorias)
   * Precio (opcional)
3. Tomar foto del recibo (solo cámara).
4. Guardar.

Mensaje clave al usuario:

> “La foto es respaldo. Los datos los controlas tú.”

---

## 6. Buscador

* Búsqueda por nombre.
* Resultado inmediato.
* Acceso rápido a la foto del recibo.

Uso crítico en emergencias (reclamos, devoluciones).

---

## 7. Almacenamiento de Imágenes

* Foto tomada desde la app.
* Compresión automática.
* Guardada en carpeta privada del sistema.
* En la base de datos solo se guarda el **path**.

---

## 8. Base de Datos (Local)

**SQLite / Room**

Tabla única: `items`

Campos principales:

* `id`
* `type` (0 = Garantía, 1 = Suscripción)
* `name`
* `price`
* `purchaseDate`
* `expiryDate`
* `billingFrequency`
* `imagePath`
* `notificationsConfig`
* `isActive`

---

## 9. Notificaciones

* Locales (OS-level).
* Configurables por tipo.
* Sin servicios en segundo plano persistentes.

---

## 10. Backup y Restauración

* Exportar copia:

  * Archivo `.zip`
  * Incluye base de datos + imágenes
* Importar copia manualmente.
* El usuario decide dónde guardarla (Drive, USB, etc).

---

## 11. Marca Personal y Pantalla "Sobre el Desarrollador"

Como KS Expire no monetiza con dinero, **monetiza en visibilidad**.

Se agrega una pantalla accesible desde el menú:

### "Sobre el Desarrollador"

Contenido:

* Foto o logo personal.
* Texto breve:

  > "Desarrollado con ❤️ por Koyere Solutions. Si te sirve esta app, compártela."

### Botones de Acción

* 🌐 Web: [https://www.koyeresolutions.com/](https://www.koyeresolutions.com/)
* 💼 LinkedIn: [https://www.linkedin.com/in/eduardo-escobar-38a888161/](https://www.linkedin.com/in/eduardo-escobar-38a888161/)
* 🧑‍💻 GitHub: [https://github.com/koyere](https://github.com/koyere)
* ⭐ Calificar App (enlace directo a la ficha de Google Play).
* 📧 Contacto / Feedback: info@koyeresolutions.com

  * Enlace `mailto:` para reportar bugs o sugerencias.

Objetivo:

* Reputación.
* Confianza.
* Puerta directa a oportunidades laborales o clientes.

---

## 12. Requisito Legal (Política de Privacidad)

* Exportar copia:

  * Archivo `.zip`
  * Incluye base de datos + imágenes
* Importar copia manualmente.
* El usuario decide dónde guardarla (Drive, USB, etc).

---

## 11. Requisito Legal (Política de Privacidad)

Aunque KS Expire es una app **offline**, Google Play **exige** una URL con Política de Privacidad debido al uso del permiso de **CÁMARA**.

### Política de Privacidad (obligatoria)

Debe existir un documento público (por ejemplo, **GitHub Pages** o sitio estático gratuito) que indique claramente:

* KS Expire utiliza la cámara **únicamente** para tomar fotos de recibos.
* Todas las fotos y datos se almacenan **localmente** en el dispositivo.
* La app **no recopila**, **no transmite** ni **comparte** información del usuario.
* No existen servidores, cuentas ni servicios en la nube.

Sin esta URL, Google **rechazará** la publicación.

---

## 13. Assets para la Tienda (Play Store)

El código no es suficiente. La presentación es crítica.

### Icono

* Diseño minimalista.
* Alto contraste.
* Identificable en tamaños pequeños.

### Feature Graphic (1024x500)

* Imagen principal de la ficha.
* Mensaje claro y corto.
* Enfoque en: control, fechas, privacidad.

### Screenshots

* Capturas reales o mockups.
* Mostrar:

  * Dashboard
  * Lista de suscripciones
  * Lista de garantías
  * Detalle con recibo

---

## 14. Nombre y Marca

**Nombre:** KS Expire

**Descripción corta:**

> Controla tus suscripciones y recibos. Todo offline. Sin cuentas.

---

## 15. Mensaje para la Store

Estrategia de comunicación:

> ¿Cansado de suscripciones, anuncios y apps que espían?
> KS Expire es diferente.
> Una herramienta creada por un desarrollador independiente que cree en la privacidad.
> Sin internet. Sin anuncios. Sin trucos.
> Solo utilidad.

> No necesitas registrarte.
> No subimos tus recibos a ningún servidor.
> Nadie analiza tus gastos.
> KS Expire vive solo en tu teléfono.

---

## 16. Roadmap Futuro (NO MVP)

* OCR local asistido (opcional).
* Modo oscuro avanzado.
* Estadísticas simples.
* Widgets.

---

**Estado del documento:** Definición cerrada para desarrollo MVP.

---

## 17. Stack de Desarrollo (Definición Técnica)

### Plataforma

* **Android (fase inicial)**
* Publicación en Google Play

---

### Lenguaje y UI

* **Kotlin**
* **Android Nativo**
* **Material Design 3**
* Arquitectura recomendada: **MVVM**

Motivo: menor complejidad, mejor rendimiento, APIs nativas estables.

---

### Persistencia de Datos

* **Room (SQLite)**
* Base de datos local
* Sin sincronización
* Sin red

Tabla principal: `items`

---

### Cámara e Imágenes

* **CameraX**
* Compresión JPEG automática (70–80%)
* Almacenamiento privado:

  * `filesDir/receipts/`

Nunca se guardan imágenes en la galería pública.

---

### Notificaciones

* **AlarmManager** (fechas exactas)
* **WorkManager** (reprogramación tras reinicio)
* Notificaciones locales únicamente

---

### Backup / Restore

* Exportación manual `.zip`
* Importación validada
* Uso de `ACTION_CREATE_DOCUMENT`

---

### Animaciones y Dinamismo (UI)

Sí, **se pueden y se deben agregar animaciones**, pero bajo estas reglas:

#### Principios

* Animaciones **funcionales**, no decorativas
* Sin afectar rendimiento
* Sin dependencias pesadas

---

### Animaciones Permitidas (MVP)

#### 1. Transiciones de pantalla

* `MaterialSharedAxis`
* `MaterialFadeThrough`

Uso:

* Dashboard → Detalle
* Crear / Editar ítem

---

#### 2. Feedback de acciones

* Ripple effects nativos
* Micro-animación al guardar / eliminar

---

#### 3. LottieFiles (uso controlado)

Permitido **solo en estos casos**:

* Estado vacío ("No tienes suscripciones")
* Pantalla inicial sin datos

Reglas:

* Animaciones cortas
* Peso ligero
* Recurso local (no remoto)

Ejemplo:

* Ícono animado de reloj / calendario

---

### Qué NO usar

* Animaciones constantes
* Fondos animados
* Lottie en listas
* Efectos que distraigan

---

### Fases de Desarrollo (Ejecución)

#### FASE 0 — Preparación

* Crear proyecto Android (Kotlin, M3)
* Package name definitivo
* Permisos mínimos

#### FASE 1 — Modelo de Datos

* Entidad `Item`
* DAO
* RoomDatabase

#### FASE 2 — CRUD Básico

* Dashboard
* Listas por tipo
* Crear / Editar / Eliminar

#### FASE 3 — Cámara

* CameraX
* Foto comprimida
* Guardado local

#### FASE 4 — Notificaciones

* Lógica de vencimientos
* Alarmas

#### FASE 5 — Buscador

* SearchView
* Query local

#### FASE 6 — Backup / Restore

* Export / Import ZIP

#### FASE 7 — Marca Personal

* Pantalla "Sobre el Desarrollador"

#### FASE 8 — Publicación

* Política de Privacidad
* Assets
* Play Store

---

**Estado:** Stack técnico y fases cerradas. Listo para desarrollo.
