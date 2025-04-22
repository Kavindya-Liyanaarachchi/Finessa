package com.example.finessa.ui.budget

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finessa.R
import com.example.finessa.adapter.CategoryBudgetAdapter
import com.example.finessa.model.CategoryBudget
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

class BudgetFragment : Fragment() {

    private lateinit var etBudget: TextInputEditText
    private lateinit var btnSaveBudget: MaterialButton
    private lateinit var rvCategoryBudgets: RecyclerView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var adapter: CategoryBudgetAdapter
    private val gson = Gson()
    private val budgetType = object : TypeToken<List<CategoryBudget>>() {}.type

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        etBudget = view.findViewById(R.id.etBudget)
        btnSaveBudget = view.findViewById(R.id.btnSaveBudget)
        rvCategoryBudgets = view.findViewById(R.id.rvCategoryBudgets)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("finance_tracker", 0)

        // Setup RecyclerView
        adapter = CategoryBudgetAdapter(
            onSave = { category, budget ->
                saveCategoryBudget(category, budget)
            }
        )
        rvCategoryBudgets.layoutManager = LinearLayoutManager(context)
        rvCategoryBudgets.adapter = adapter

        // Load saved data
        loadSavedData()

        // Setup save button
        btnSaveBudget.setOnClickListener {
            val budget = etBudget.text.toString().toDoubleOrNull()
            if (budget == null) {
                Toast.makeText(context, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveMonthlyBudget(budget)
            Toast.makeText(context, "Budget saved successfully", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedData() {
        // Load monthly budget
        val monthlyBudget = sharedPreferences.getFloat("monthly_budget", 0f)
        if (monthlyBudget > 0) {
            etBudget.setText(monthlyBudget.toString())
        }

        // Load category budgets
        val json = sharedPreferences.getString("category_budgets", null)
        val budgets = if (json != null) {
            gson.fromJson<List<CategoryBudget>>(json, budgetType)
        } else {
            emptyList()
        }
        adapter.updateBudgets(budgets)
    }

    private fun saveMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat("monthly_budget", budget.toFloat()).apply()
    }

    private fun saveCategoryBudget(category: String, budget: Double) {
        val json = sharedPreferences.getString("category_budgets", null)
        val budgets = if (json != null) {
            gson.fromJson<MutableList<CategoryBudget>>(json, budgetType)
        } else {
            mutableListOf()
        }

        val existingBudget = budgets.find { it.category == category }
        if (existingBudget != null) {
            existingBudget.budget = budget
        } else {
            budgets.add(CategoryBudget(category, budget))
        }

        sharedPreferences.edit()
            .putString("category_budgets", gson.toJson(budgets))
            .apply()

        adapter.updateBudgets(budgets)
    }
}