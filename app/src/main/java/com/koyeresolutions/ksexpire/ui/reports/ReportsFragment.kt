package com.koyeresolutions.ksexpire.ui.reports

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.snackbar.Snackbar
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.databinding.FragmentReportsBinding
import com.koyeresolutions.ksexpire.utils.Constants
import com.koyeresolutions.ksexpire.utils.CurrencyUtils

/**
 * Fragment de reportes con gráficas y resumen financiero
 */
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportsViewModel by viewModels()

    // Colores para las gráficas
    private val chartColors = listOf(
        Color.parseColor("#1976D2"), // Azul
        Color.parseColor("#E91E63"), // Rosa
        Color.parseColor("#9C27B0"), // Púrpura
        Color.parseColor("#4CAF50"), // Verde
        Color.parseColor("#FF9800"), // Naranja
        Color.parseColor("#00BCD4"), // Cyan
        Color.parseColor("#673AB7"), // Violeta
        Color.parseColor("#F44336"), // Rojo
        Color.parseColor("#795548"), // Marrón
        Color.parseColor("#607D8B"), // Gris
        Color.parseColor("#3F51B5"), // Índigo
        Color.parseColor("#8BC34A")  // Verde claro
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.reportData.observe(viewLifecycleOwner) { data ->
            updateSummary(data)
            updateCategoryChart(data.expenseByCategory)
            updateDistributionChart(data.expenseByCategory)
            updateTopExpenses(data.topSubscriptions)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.buttonShareReport.setOnClickListener {
            // Se implementará en Fase 9 (PDF)
            Snackbar.make(binding.root, "Próximamente: compartir reporte en PDF", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun updateSummary(data: ReportsViewModel.ReportData) {
        binding.textReportMonthly.text = CurrencyUtils.formatPrice(requireContext(), data.monthlyExpense)
        binding.textReportAnnual.text = CurrencyUtils.formatPrice(requireContext(), data.annualProjection)
        binding.textReportSubsCount.text = getString(R.string.reports_subs_count, data.subscriptionsCount)
        binding.textReportWarrCount.text = getString(R.string.reports_warr_count, data.warrantiesCount)
    }

    private fun updateCategoryChart(expenseByCategory: Map<String, Double>) {
        if (expenseByCategory.isEmpty()) {
            binding.chartCategories.visibility = View.GONE
            binding.textNoCategoryData.visibility = View.VISIBLE
            return
        }
        binding.chartCategories.visibility = View.VISIBLE
        binding.textNoCategoryData.visibility = View.GONE

        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val colors = mutableListOf<Int>()

        expenseByCategory.entries.sortedByDescending { it.value }.forEachIndexed { index, (category, amount) ->
            entries.add(BarEntry(index.toFloat(), amount.toFloat()))
            // Buscar emoji de la categoría
            val predefined = Constants.PREDEFINED_CATEGORIES.find { it.name == category }
            labels.add(if (predefined != null) "${predefined.emoji} $category" else category)
            // Usar color de la categoría o color por defecto
            val catColor = predefined?.color?.let { Color.parseColor(it) } ?: chartColors[index % chartColors.size]
            colors.add(catColor)
        }

        val dataSet = BarDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 11f
            valueTextColor = getTextColor()
            setDrawValues(true)
        }

        val isDark = isDarkMode()
        binding.chartCategories.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setFitBars(true)
            setDrawGridBackground(false)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textColor = getTextColor()
                textSize = 10f
            }
            axisLeft.apply {
                axisMinimum = 0f
                textColor = getTextColor()
                setDrawGridLines(true)
                gridColor = if (isDark) Color.parseColor("#333333") else Color.parseColor("#E0E0E0")
            }
            axisRight.isEnabled = false
            animateY(800, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun updateDistributionChart(expenseByCategory: Map<String, Double>) {
        if (expenseByCategory.isEmpty()) {
            binding.chartDistribution.visibility = View.GONE
            return
        }
        binding.chartDistribution.visibility = View.VISIBLE

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()
        val total = expenseByCategory.values.sum()

        expenseByCategory.entries.sortedByDescending { it.value }.forEachIndexed { index, (category, amount) ->
            val percentage = (amount / total * 100).toFloat()
            val predefined = Constants.PREDEFINED_CATEGORIES.find { it.name == category }
            val label = predefined?.emoji?.let { "$it $category" } ?: category
            entries.add(PieEntry(percentage, label))
            val catColor = predefined?.color?.let { Color.parseColor(it) } ?: chartColors[index % chartColors.size]
            colors.add(catColor)
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            sliceSpace = 2f
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter()
        }

        binding.chartDistribution.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 45f
            transparentCircleRadius = 50f
            setHoleColor(Color.TRANSPARENT)
            setCenterText("Total\n${CurrencyUtils.formatPrice(requireContext(), total)}")
            setCenterTextSize(12f)
            setCenterTextColor(getTextColor())
            setEntryLabelColor(getTextColor())
            setEntryLabelTextSize(10f)
            legend.apply {
                isEnabled = true
                textColor = getTextColor()
                textSize = 10f
            }
            setUsePercentValues(true)
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun updateTopExpenses(topSubscriptions: List<Item>) {
        val container = binding.layoutTopExpenses
        container.removeAllViews()

        if (topSubscriptions.isEmpty()) {
            binding.textNoExpenses.visibility = View.VISIBLE
            return
        }
        binding.textNoExpenses.visibility = View.GONE

        topSubscriptions.forEachIndexed { index, item ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 12, 0, 12)
            }

            // Número de ranking
            val rankText = TextView(requireContext()).apply {
                text = "${index + 1}."
                textSize = 16f
                setTextColor(getTextColor())
                setPadding(0, 0, 16, 0)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            // Nombre
            val nameText = TextView(requireContext()).apply {
                text = item.name
                textSize = 14f
                setTextColor(getTextColor())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Precio mensual
            val priceText = TextView(requireContext()).apply {
                text = CurrencyUtils.formatSubscriptionPrice(requireContext(), item.price, item.billingFrequency)
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.subscription_primary))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            row.addView(rankText)
            row.addView(nameText)
            row.addView(priceText)
            container.addView(row)

            // Divider (excepto el último)
            if (index < topSubscriptions.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    )
                    val dividerColor = android.util.TypedValue()
                    requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, dividerColor, true)
                    setBackgroundColor(requireContext().getColor(dividerColor.resourceId))
                }
                container.addView(divider)
            }
        }
    }

    private fun getTextColor(): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        return requireContext().getColor(typedValue.resourceId)
    }

    private fun isDarkMode(): Boolean {
        val nightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReportData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
