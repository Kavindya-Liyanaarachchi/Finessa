package com.example.finessa.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finessa.R
import com.example.finessa.model.CategoryBudget
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.util.Locale

class CategoryBudgetAdapter(
    private val onSave: (String, Double) -> Unit
) : RecyclerView.Adapter<CategoryBudgetAdapter.CategoryBudgetViewHolder>() {

    private var budgets: List<CategoryBudget> = emptyList()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    class CategoryBudgetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val etBudget: TextInputEditText = itemView.findViewById(R.id.etBudget)
        val btnSave: MaterialButton = itemView.findViewById(R.id.btnSave)
        val progressBar: LinearProgressIndicator = itemView.findViewById(R.id.progressBar)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryBudgetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_budget, parent, false)
        return CategoryBudgetViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryBudgetViewHolder, position: Int) {
        val budget = budgets[position]
        holder.tvCategory.text = budget.category
        holder.etBudget.setText(budget.budget.toString())

        // Calculate progress (example: 75% spent)
        val progress = 75 // This should be calculated based on actual spending
        holder.progressBar.progress = progress
        holder.tvProgress.text = "$${budget.budget} • $progress% spent"

        holder.btnSave.setOnClickListener {
            val newBudget = holder.etBudget.text.toString().toDoubleOrNull()
            if (newBudget != null) {
                onSave(budget.category, newBudget)
            }
        }
    }

    override fun getItemCount() = budgets.size

    fun updateBudgets(newBudgets: List<CategoryBudget>) {
        budgets = newBudgets
        notifyDataSetChanged()
    }
} 