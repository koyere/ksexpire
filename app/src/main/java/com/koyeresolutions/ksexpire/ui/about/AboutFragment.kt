package com.koyeresolutions.ksexpire.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            📦 Package: ${requireContext().packageName}
            🛡️ 100% Offline y Privado
            💾 Sin cuentas ni servidores
            🔒 Datos almacenados localmente
            
            Desarrollado con ❤️ usando:
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}