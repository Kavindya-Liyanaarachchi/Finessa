package com.example.finessa.ui.transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.finessa.R
import com.example.finessa.databinding.FragmentAddTransactionBinding
import com.example.finessa.model.Transaction
import com.google.android.material.snackbar.Snackbar
import java.util.Date

class AddTransactionFragment : Fragment() {
    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCategoryDropdown()
        setupTypeSelection()
        setupSaveButton()
    }

    private fun setupCategoryDropdown() {
        // Initially set up with expense categories
        updateCategoryDropdown(false)
    }

    private fun updateCategoryDropdown(isIncome: Boolean) {
        val categories = if (isIncome) {
            resources.getStringArray(R.array.income_categories)
        } else {
            resources.getStringArray(R.array.expense_categories)
        }
        
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_menu_item,
            categories
        )
        binding.actvCategory.setAdapter(adapter)
    }

    private fun setupTypeSelection() {
        binding.cgType.setOnCheckedChangeListener { _, checkedId ->
            val isIncome = binding.chipIncome.isChecked
            updateCategoryDropdown(isIncome)
            // Clear the current category selection when switching types
            binding.actvCategory.text.clear()
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                saveTransaction()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validate title
        if (binding.etTitle.text.isNullOrEmpty()) {
            binding.tilTitle.error = "Title is required"
            isValid = false
        } else {
            binding.tilTitle.error = null
        }

        // Validate amount
        if (binding.etAmount.text.isNullOrEmpty()) {
            binding.tilAmount.error = "Amount is required"
            isValid = false
        } else {
            try {
                binding.etAmount.text.toString().toDouble()
                binding.tilAmount.error = null
            } catch (e: NumberFormatException) {
                binding.tilAmount.error = "Invalid amount"
                isValid = false
            }
        }

        // Validate category
        if (binding.actvCategory.text.isNullOrEmpty()) {
            binding.tilCategory.error = "Category is required"
            isValid = false
        } else {
            binding.tilCategory.error = null
        }

        return isValid
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text.toString()
        val amount = binding.etAmount.text.toString().toDouble()
        val category = binding.actvCategory.text.toString()
        val isIncome = binding.chipIncome.isChecked

        // Create transaction object with all required parameters
        val transaction = Transaction(
            id = 0, // Temporary ID, will be replaced by database
            title = title,
            amount = amount,
            category = category,
            date = Date(),
            isIncome = isIncome,
            description = "", // Optional parameter with default value
            recurring = false // Optional parameter with default value
        )

        // Save the transaction and show success message
        Snackbar.make(binding.root, "Transaction saved successfully", Snackbar.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 