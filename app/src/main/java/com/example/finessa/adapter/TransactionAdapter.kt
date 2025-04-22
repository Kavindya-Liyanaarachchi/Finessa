package com.example.finessa.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.finessa.R
import com.example.finessa.model.Transaction
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private var transactions: List<Transaction> = emptyList(),
    private val onItemClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val category: TextView = itemView.findViewById(R.id.tvCategory)
        val amount: TextView = itemView.findViewById(R.id.tvAmount)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.title.text = transaction.title
        holder.category.text = "${transaction.category} • ${dateFormat.format(transaction.date)}"
        holder.amount.text = if (transaction.isIncome) {
            "+$${String.format("%.2f", transaction.amount)}"
        } else {
            "-$${String.format("%.2f", transaction.amount)}"
        }
        holder.amount.setTextColor(
            holder.itemView.context.getColor(
                if (transaction.isIncome) R.color.success else R.color.error
            )
        )

        // Set up click listeners
        holder.itemView.setOnClickListener { onItemClick(transaction) }
        holder.deleteButton.setOnClickListener { onDeleteClick(transaction) }
    }

    override fun getItemCount() = transactions.size

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    // Expose transactions list for external access
    val transactionsList: List<Transaction>
        get() = transactions
}