package com.example.finessa.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.finessa.R
import com.example.finessa.model.Transaction
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Date

class TransactionDialogFragment : DialogFragment() {
    private var existingTransaction: Transaction? = null
    private var onSave: ((Transaction) -> Unit)? = null
    var isIncome = false  // Make this public

    fun setTransaction(transaction: Transaction) {
        existingTransaction = transaction
        isIncome = transaction.isIncome
    }

    fun setOnSaveListener(listener: (Transaction) -> Unit) {
        onSave = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etTitle = view.findViewById<TextInputEditText>(R.id.etTitle)
        val etAmount = view.findViewById<TextInputEditText>(R.id.etAmount)
        val actvCategory = view.findViewById<AutoCompleteTextView>(R.id.actvCategory)
        val etDescription = view.findViewById<TextInputEditText>(R.id.etDescription)
        val switchRecurring = view.findViewById<SwitchMaterial>(R.id.switchRecurring)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        // Setup category dropdown based on transaction type
        val categories = if (isIncome) {
            resources.getStringArray(R.array.income_categories)
        } else {
            resources.getStringArray(R.array.expense_categories)
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        actvCategory.setAdapter(adapter)

        // If editing existing transaction, populate fields
        existingTransaction?.let { transaction ->
            etTitle.setText(transaction.title)
            etAmount.setText(transaction.amount.toString())
            actvCategory.setText(transaction.category)
            etDescription.setText(transaction.description)
            switchRecurring.isChecked = transaction.recurring
        }

        btnCancel.setOnClickListener { dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString()
            val amount = etAmount.text.toString().toDoubleOrNull()
            val category = actvCategory.text.toString()
            val description = etDescription.text.toString()
            val recurring = switchRecurring.isChecked

            if (title.isBlank() || amount == null || category.isBlank()) {
                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val transaction = existingTransaction?.copy(
                title = title,
                amount = amount,
                category = category,
                description = description,
                recurring = recurring
            ) ?: Transaction(
                id = 0,
                title = title,
                amount = amount,
                category = category,
                date = Date(),
                isIncome = isIncome,
                description = description,
                recurring = recurring
            )

            onSave?.invoke(transaction)
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
} 