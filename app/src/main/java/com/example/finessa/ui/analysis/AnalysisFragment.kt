package com.example.finessa.ui.analysis

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.finessa.R
import com.example.finessa.databinding.FragmentAnalysisBinding
import com.example.finessa.model.Transaction
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AnalysisFragment : Fragment() {
    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    // Define custom colors for categories
    private val expenseCategoryColors = listOf(
        Color.parseColor("#FF6B6B"),  // Red for Food
        Color.parseColor("#4ECDC4"),  // Teal for Transport
        Color.parseColor("#45B7D1"),  // Blue for Entertainment
        Color.parseColor("#96CEB4"),  // Green for Bills
        Color.parseColor("#9D81BA")   // Purple for Other
    )

    private val incomeCategoryColors = listOf(
        Color.parseColor("#4CAF50"),  // Green for Salary
        Color.parseColor("#2196F3"),  // Blue for Investments
        Color.parseColor("#FFC107"),  // Yellow for Freelance
        Color.parseColor("#9C27B0"),  // Purple for Gifts
        Color.parseColor("#FF9800")   // Orange for Other
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        sharedPreferences = requireContext().getSharedPreferences("finance_tracker", 0)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
    }

    private fun setupCharts() {
        val transactions = loadTransactions()
        setupExpensePieChart(transactions.filter { !it.isIncome })
        setupIncomePieChart(transactions.filter { it.isIncome })
    }

    private fun setupExpensePieChart(expenses: List<Transaction>) {
        val categoryMap = expenses.groupBy { it.category }
        val entries = ArrayList<PieEntry>()
        var totalExpenses = 0.0

        categoryMap.forEach { (category, transactions) ->
            val total = transactions.sumOf { it.amount }
            totalExpenses += total
            entries.add(PieEntry(total.toFloat(), category))
        }

        val dataSet = PieDataSet(entries, "Expense Categories")
        dataSet.colors = expenseCategoryColors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        binding.expensePieChart.apply {
            this.data = data
            description.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            animateY(1000)
            legend.textSize = 12f
            legend.formSize = 12f
            setUsePercentValues(true)
            data.setValueFormatter(PercentFormatter(this))
        }

        // Update expense summary
        binding.tvTotalExpenses.text = "Total Expenses: $${String.format("%.2f", totalExpenses)}"
        val largestCategory = categoryMap.maxByOrNull { it.value.sumOf { t -> t.amount } }
        binding.tvLargestExpenseCategory.text = "Largest Category: ${largestCategory?.key ?: "-"}"
    }

    private fun setupIncomePieChart(incomes: List<Transaction>) {
        val categoryMap = incomes.groupBy { it.category }
        val entries = ArrayList<PieEntry>()
        var totalIncome = 0.0

        categoryMap.forEach { (category, transactions) ->
            val total = transactions.sumOf { it.amount }
            totalIncome += total
            entries.add(PieEntry(total.toFloat(), category))
        }

        val dataSet = PieDataSet(entries, "Income Categories")
        dataSet.colors = incomeCategoryColors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)
        binding.incomePieChart.apply {
            this.data = data
            description.isEnabled = false
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            legend.isEnabled = true
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            animateY(1000)
            legend.textSize = 12f
            legend.formSize = 12f
            setUsePercentValues(true)
            data.setValueFormatter(PercentFormatter(this))
        }

        // Update income summary
        binding.tvTotalIncome.text = "Total Income: $${String.format("%.2f", totalIncome)}"
        val largestCategory = categoryMap.maxByOrNull { it.value.sumOf { t -> t.amount } }
        binding.tvLargestIncomeCategory.text = "Largest Category: ${largestCategory?.key ?: "-"}"
    }

    private fun loadTransactions(): List<Transaction> {
        val json = sharedPreferences.getString("transactions", null)
        return if (json != null) {
            gson.fromJson(json, object : TypeToken<List<Transaction>>() {}.type)
        } else {
            emptyList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}