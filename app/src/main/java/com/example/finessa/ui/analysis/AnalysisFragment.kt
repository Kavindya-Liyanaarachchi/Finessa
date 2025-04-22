package com.example.finessa.ui.analysis

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.finessa.R
import com.example.finessa.databinding.FragmentAnalysisBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class AnalysisFragment : Fragment() {
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    // Define custom colors for each category
    private val categoryColors = listOf(
        Color.parseColor("#FF6B6B"),  // Red for Food
        Color.parseColor("#4ECDC4"),  // Teal for Transport
        Color.parseColor("#45B7D1"),  // Blue for Entertainment
        Color.parseColor("#96CEB4"),  // Green for Bills
        Color.parseColor("#9D81BA")   // Purple for Other
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPieChart()
    }

    private fun setupPieChart() {
        // Sample data - replace with your actual transaction data
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(30f, "Food"))
        entries.add(PieEntry(20f, "Transport"))
        entries.add(PieEntry(15f, "Entertainment"))
        entries.add(PieEntry(25f, "Bills"))
        entries.add(PieEntry(10f, "Other"))

        val dataSet = PieDataSet(entries, "Spending Categories")
        dataSet.colors = categoryColors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        binding.pieChart.data = data

        // Customize the chart appearance
        binding.pieChart.apply {
            description.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            animateY(1000)
            
            // Additional customization for better visibility
            legend.textSize = 12f
            legend.formSize = 12f
            setUsePercentValues(true)
            data.setValueFormatter(com.github.mikephil.charting.formatter.PercentFormatter(this))
        }

        // Update text views with analysis data
        val totalSpending = entries.sumOf { it.value.toDouble() }
        binding.tvTotalSpending.text = "Total Spending: $${String.format("%.2f", totalSpending)}"
        
        val largestCategory = entries.maxByOrNull { it.value }
        binding.tvLargestCategory.text = "Largest Category: ${largestCategory?.label ?: "-"}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}