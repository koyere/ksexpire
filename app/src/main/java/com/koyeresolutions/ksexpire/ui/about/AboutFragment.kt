package com.koyeresolutions.ksexpire.ui.about

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.databinding.FragmentAboutBinding
import com.koyeresolutions.ksexpire.utils.Constants

/**
 * Fragment "Sobre el Desarrollador"
 * IMPLEMENTA FUNCIONALIDAD COMPLETA DEL PLANNING CON IN-APP REVIEW
 */
class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AboutViewModel by viewModels()

    // Launcher para solicitar permiso de notificaciones
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showSuccess("Notificaciones activadas correctamente")
        } else {
            showNotificationPermissionDeniedDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
        setupObservers()
    }

    /**
     * Configurar elementos de UI
     */
    private fun setupUI() {
        // Configurar información de la app
        binding.apply {
            textAppVersion.text = "Versión ${getAppVersion()}"
            textDeveloperDescription.text = getString(R.string.about_description)
        }
    }

    /**
     * Configurar listeners de clicks
     */
    private fun setupClickListeners() {
        binding.apply {
            // Sitio web
            cardWebsite.setOnClickListener {
                openUrl(Constants.DEVELOPER_WEBSITE)
            }

            // LinkedIn
            cardLinkedin.setOnClickListener {
                openLinkedIn()
            }

            // GitHub
            cardGithub.setOnClickListener {
                openGitHub()
            }

            // Discord
            cardDiscord.setOnClickListener {
                openUrl(Constants.DEVELOPER_DISCORD)
            }

            // Calificar app (In-App Review)
            cardRateApp.setOnClickListener {
                viewModel.requestInAppReview(requireActivity())
            }

            // Contacto por email
            cardContact.setOnClickListener {
                openEmailContact()
            }

            // Información adicional
            cardAppInfo.setOnClickListener {
                showAppInfo()
            }

            // Configuración de notificaciones
            cardNotifications.setOnClickListener {
                showNotificationSettings()
            }
        }
    }

    /**
     * Configurar observadores
     */
    private fun setupObservers() {
        // Observar resultado de review
        viewModel.reviewResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (!it) {
                    // Si falla el in-app review, abrir Play Store
                    openPlayStore()
                }
                viewModel.clearReviewResult()
            }
        }

        // Observar errores
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }
    }

    /**
     * Abrir URL en navegador
     */
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            showError("No se pudo abrir el enlace")
        }
    }

    /**
     * Abrir LinkedIn (app nativa o navegador)
     */
    private fun openLinkedIn() {
        try {
            // Intentar abrir en app nativa de LinkedIn
            val linkedInIntent = Intent(Intent.ACTION_VIEW)
            linkedInIntent.data = Uri.parse("linkedin://profile/eduardo-escobar-38a888161")
            
            if (linkedInIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(linkedInIntent)
            } else {
                // Si no tiene la app, abrir en navegador
                openUrl(Constants.DEVELOPER_LINKEDIN)
            }
        } catch (e: Exception) {
            openUrl(Constants.DEVELOPER_LINKEDIN)
        }
    }

    /**
     * Abrir GitHub (app nativa o navegador)
     */
    private fun openGitHub() {
        try {
            // Intentar abrir en app nativa de GitHub
            val githubIntent = Intent(Intent.ACTION_VIEW)
            githubIntent.data = Uri.parse("github://koyere")
            
            if (githubIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(githubIntent)
            } else {
                // Si no tiene la app, abrir en navegador
                openUrl(Constants.DEVELOPER_GITHUB)
            }
        } catch (e: Exception) {
            openUrl(Constants.DEVELOPER_GITHUB)
        }
    }

    /**
     * Abrir Play Store para calificar
     */
    private fun openPlayStore() {
        try {
            val playStoreIntent = Intent(Intent.ACTION_VIEW)
            playStoreIntent.data = Uri.parse("market://details?id=${requireContext().packageName}")
            
            if (playStoreIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(playStoreIntent)
            } else {
                // Si no tiene Play Store, abrir en navegador
                val webIntent = Intent(Intent.ACTION_VIEW)
                webIntent.data = Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}")
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            showError("No se pudo abrir Play Store")
        }
    }

    /**
     * Abrir email de contacto
     */
    private fun openEmailContact() {
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(Constants.DEVELOPER_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_contact_subject))
                putExtra(Intent.EXTRA_TEXT, generateEmailBody())
            }
            
            if (emailIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(Intent.createChooser(emailIntent, "Enviar feedback"))
            } else {
                showError("No se encontró una app de email")
            }
        } catch (e: Exception) {
            showError("No se pudo abrir el email")
        }
    }

    /**
     * Mostrar información adicional de la app
     */
    private fun showAppInfo() {
        val info = """
            📱 KS Expire
            🔢 Versión: ${getAppVersion()}
            🛡️ 100% Offline y Privado
            💾 Sin cuentas ni servidores
            🔒 Datos almacenados localmente
            
            Desarrollado por Koyere Dev usando:
            • Kotlin & Android Nativo
            • Material Design 3
            • Room Database
            • CameraX
            • WorkManager
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Información de la App")
            .setMessage(info)
            .setPositiveButton("Aceptar", null)
            .setIcon(R.drawable.ic_info)
            .show()
    }

    /**
     * Generar cuerpo del email de contacto
     */
    private fun generateEmailBody(): String {
        return """
            Hola Eduardo,
            
            Te escribo sobre KS Expire:
            
            [Escribe tu mensaje aquí]
            
            ---
            Información técnica:
            • Versión de la app: ${getAppVersion()}
            • Dispositivo: ${android.os.Build.MODEL}
            • Android: ${android.os.Build.VERSION.RELEASE}
            • Package: ${requireContext().packageName}
        """.trimIndent()
    }

    /**
     * Obtener versión de la app
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Mostrar mensaje de error
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Mostrar mensaje de éxito
     */
    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.brand_green))
            .show()
    }

    /**
     * Mostrar configuración de notificaciones
     */
    private fun showNotificationSettings() {
        val areNotificationsEnabled = areNotificationsEnabled()
        
        val statusIcon = if (areNotificationsEnabled) "✅" else "❌"
        val statusText = if (areNotificationsEnabled) "Activadas" else "Desactivadas"
        
        val message = """
            Estado actual: $statusIcon $statusText
            
            Las notificaciones te avisan:
            • 1 día antes del cobro de suscripciones
            • 30 y 7 días antes del vencimiento de garantías
            
            ${if (!areNotificationsEnabled) "⚠️ Activa las notificaciones para no perderte ningún vencimiento." else ""}
        """.trimIndent()
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🔔 Notificaciones")
            .setMessage(message)
            .setIcon(R.drawable.ic_notifications)
        
        if (areNotificationsEnabled) {
            builder.setPositiveButton("Aceptar", null)
            builder.setNeutralButton("Configuración") { _, _ ->
                openNotificationSettings()
            }
        } else {
            builder.setPositiveButton("Activar") { _, _ ->
                requestNotificationPermission()
            }
            builder.setNegativeButton("Cancelar", null)
        }
        
        builder.show()
    }

    /**
     * Verificar si las notificaciones están habilitadas
     */
    private fun areNotificationsEnabled(): Boolean {
        val notificationManager = requireContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE) 
            as android.app.NotificationManager
        
        // Android 13+ (API 33) requiere permiso POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            return hasPermission && notificationManager.areNotificationsEnabled()
        }
        
        // Android 8+ verifica si las notificaciones están habilitadas
        return notificationManager.areNotificationsEnabled()
    }

    /**
     * Solicitar permiso de notificaciones
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requiere solicitar permiso explícito
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya tiene permiso, verificar si están habilitadas en sistema
                    openNotificationSettings()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Usuario rechazó antes, mostrar explicación
                    showNotificationPermissionRationale()
                }
                else -> {
                    // Solicitar permiso
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android < 13, abrir configuración del sistema
            openNotificationSettings()
        }
    }

    /**
     * Mostrar explicación de por qué se necesitan notificaciones
     */
    private fun showNotificationPermissionRationale() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Permiso necesario")
            .setMessage("Las notificaciones son necesarias para avisarte antes de que venzan tus suscripciones y garantías.\n\n¿Deseas activarlas?")
            .setPositiveButton("Activar") { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(R.drawable.ic_notifications)
            .show()
    }

    /**
     * Mostrar diálogo cuando el permiso fue denegado
     */
    private fun showNotificationPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Permiso denegado")
            .setMessage("Para recibir recordatorios de vencimientos, debes activar las notificaciones manualmente en la configuración del sistema.")
            .setPositiveButton("Ir a Configuración") { _, _ ->
                openNotificationSettings()
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(R.drawable.ic_notifications)
            .show()
    }

    /**
     * Abrir configuración de notificaciones del sistema
     */
    private fun openNotificationSettings() {
        try {
            val intent = Intent().apply {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    }
                    else -> {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.parse("package:${requireContext().packageName}")
                    }
                }
            }
            startActivity(intent)
        } catch (e: Exception) {
            showError("No se pudo abrir la configuración")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}