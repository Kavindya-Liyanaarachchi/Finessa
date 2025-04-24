package com.example.finessa.ui.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finessa.R
import com.example.finessa.adapter.TransactionAdapter
import com.example.finessa.model.Transaction
import com.example.finessa.ui.dialog.TransactionDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Date
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment() {

    private lateinit var adapter: TransactionAdapter
    private lateinit var tvBalance: TextView
    private lateinit var tvBudgetStatus: TextView
    private lateinit var progressBudget: LinearProgressIndicator
    private lateinit var cardBudgetStatus: MaterialCardView
    private lateinit var rvTransactions: RecyclerView
    private lateinit var fabAddTransaction: FloatingActionButton
    private lateinit var btnAddIncome: MaterialButton
    private lateinit var btnAddExpense: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val transactionType = object : TypeToken<List<Transaction>>() {}.type
    private val CHANNEL_ID = "budget_notification_channel"
    private val NOTIFICATION_ID = 1

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            showSnackbar("Notifications are disabled. You won't receive budget alerts.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createNotificationChannel()
        checkNotificationPermission()

        // Initialize views
        tvBalance = view.findViewById(R.id.tvBalance)
        tvBudgetStatus = view.findViewById(R.id.tvBudgetStatus)
        progressBudget = view.findViewById(R.id.progressBudget)
        cardBudgetStatus = view.findViewById(R.id.cardBudgetStatus)
        rvTransactions = view.findViewById(R.id.rvTransactions)
        fabAddTransaction = view.findViewById(R.id.fabAddTransaction)
        btnAddIncome = view.findViewById(R.id.btnAddIncome)
        btnAddExpense = view.findViewById(R.id.btnAddExpense)

        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("finance_tracker", 0)

        // Setup RecyclerView
        adapter = TransactionAdapter(
            onItemClick = { transaction ->
                showTransactionDialog(isIncome = transaction.isIncome, existingTransaction = transaction)
            },
            onDeleteClick = { transaction ->
                showDeleteConfirmationDialog(transaction)
            }
        )
        rvTransactions.layoutManager = LinearLayoutManager(context)
        rvTransactions.adapter = adapter

        // Setup buttons
        btnAddIncome.setOnClickListener { showTransactionDialog(isIncome = true) }
        btnAddExpense.setOnClickListener { showTransactionDialog(isIncome = false) }
        fabAddTransaction.setOnClickListener { showTransactionDialog(isIncome = false) }

        // Load saved data
        loadSavedData()
        updateBudgetStatus()
    }

    private fun showTransactionDialog(isIncome: Boolean, existingTransaction: Transaction? = null) {
        val dialog = TransactionDialogFragment()
        dialog.isIncome = isIncome  // Set the isIncome flag
        if (existingTransaction != null) {
            dialog.setTransaction(existingTransaction)
        }
        dialog.setOnSaveListener { transaction ->
            val transactions = adapter.transactionsList.toMutableList()
            
            if (!isIncome) {
                if (!validateBudgets(transaction, transactions)) {
                    return@setOnSaveListener
                }
            }

            if (existingTransaction != null) {
                val index = transactions.indexOfFirst { it.id == existingTransaction.id }
                if (index != -1) {
                    transactions[index] = transaction
                    showSnackbar("Transaction updated successfully")
                }
            } else {
                transactions.add(transaction)
                showSnackbar("Transaction added successfully")
            }

            adapter.updateTransactions(transactions)
            saveData(transactions)
            updateBudgetStatus()
        }
        dialog.show(childFragmentManager, "transaction_dialog")
    }

    private fun validateBudgets(newTransaction: Transaction, transactions: List<Transaction>): Boolean {
        // Monthly Budget Validation
        val monthlyBudget = sharedPreferences.getFloat("monthly_budget", 0f)
        if (monthlyBudget > 0) {
            val currentMonthExpenses = getCurrentMonthExpenses(transactions)
            val newTotal = currentMonthExpenses + newTransaction.amount
            if (newTotal > monthlyBudget) {
                showBudgetLimitBlockedNotification(newTotal, monthlyBudget.toDouble(), "Monthly")
                return false
            }
        }

        // Weekly Budget Validation
        val weeklyBudget = sharedPreferences.getFloat("weekly_budget", 0f)
        if (weeklyBudget > 0) {
            val currentWeekExpenses = getCurrentWeekExpenses(transactions)
            val newWeeklyTotal = currentWeekExpenses + newTransaction.amount
            if (newWeeklyTotal > weeklyBudget) {
                showBudgetLimitBlockedNotification(newWeeklyTotal, weeklyBudget.toDouble(), "Weekly")
                return false
            }
        }

        // Category Budget Validation
        val categoryBudgetJson = sharedPreferences.getString("category_budgets", null)
        if (categoryBudgetJson != null) {
            val categoryBudgets = gson.fromJson<Map<String, Double>>(categoryBudgetJson, object : TypeToken<Map<String, Double>>() {}.type)
            val categoryBudget = categoryBudgets[newTransaction.category]
            if (categoryBudget != null) {
                val currentCategoryExpenses = getCurrentMonthCategoryExpenses(transactions, newTransaction.category)
                val newCategoryTotal = currentCategoryExpenses + newTransaction.amount
                if (newCategoryTotal > categoryBudget) {
                    showBudgetLimitBlockedNotification(newCategoryTotal, categoryBudget, newTransaction.category)
                    return false
                }
            }
        }

        return true
    }

    private fun getCurrentWeekExpenses(transactions: List<Transaction>): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val weekStart = calendar.time

        return transactions
            .filter { !it.isIncome && it.date >= weekStart }
            .sumOf { it.amount }
    }

    private fun getCurrentMonthExpenses(transactions: List<Transaction>): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return transactions
            .filter { !it.isIncome } // Only expenses
            .filter {
                val transactionCalendar = Calendar.getInstance().apply { time = it.date }
                transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                transactionCalendar.get(Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount }
    }

    private fun getCurrentMonthCategoryExpenses(transactions: List<Transaction>, category: String): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return transactions
            .filter { !it.isIncome && it.category == category }
            .filter {
                val transactionCalendar = Calendar.getInstance().apply { time = it.date }
                transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                transactionCalendar.get(Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount }
    }

    private fun updateBudgetStatus() {
        val monthlyBudget = sharedPreferences.getFloat("monthly_budget", 0f)
        if (monthlyBudget > 0) {
            val currentExpenses = getCurrentMonthExpenses(adapter.transactionsList)
            val remainingBudget = monthlyBudget.toDouble() - currentExpenses
            val percentage = (currentExpenses / monthlyBudget.toDouble() * 100).toInt()
            
            progressBudget.progress = percentage
            tvBudgetStatus.text = buildString {
                append("Monthly Budget Status:\n")
                append("Spent: $${String.format("%.2f", currentExpenses)}\n")
                append("Budget: $${String.format("%.2f", monthlyBudget)}\n")
                append("Remaining: $${String.format("%.2f", remainingBudget)}\n")
                append("(${percentage}% used)")
            }
            
            // Update card color based on budget status
            val cardColor = when {
                percentage >= 100 -> R.color.error_light
                percentage >= 80 -> R.color.warning_light
                else -> R.color.success_light
            }
            cardBudgetStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), cardColor))

            // Show warning if approaching limit (80% or more)
            if (percentage >= 80 && percentage < 100) {
                showSnackbar("Warning: You've used ${percentage}% of your monthly budget!")
            }
            // Show alert if exceeded (100% or more)
            else if (percentage >= 100) {
                showSnackbar("Alert: You've exceeded your monthly budget!")
            }
        } else {
            tvBudgetStatus.text = "Set a monthly budget in the Budget tab to track your spending"
            cardBudgetStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface))
            progressBudget.progress = 0
        }
    }

    private fun showBudgetLimitBlockedNotification(currentAmount: Double, budgetLimit: Double, budgetType: String) {
        // Check if we have notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showBudgetLimitMessage(currentAmount, budgetLimit, budgetType)
            return
        }

        // Intent to open the Budget Fragment
        val budgetIntent = Intent(requireContext(), MainActivity::class.java).apply {
            action = "NAVIGATE_TO_BUDGET"
        }
        val budgetPendingIntent = PendingIntent.getActivity(
            requireContext(),
            1,
            budgetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val percentage = (currentAmount / budgetLimit * 100).toInt()
        val overAmount = currentAmount - budgetLimit

        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_warning_24)
            .setContentTitle("Budget Limit Message")
            .setContentText("$budgetType budget limit exceeded by $${String.format("%.2f", overAmount)}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your $budgetType budget has been exceeded by $${String.format("%.2f", overAmount)}.\n\n" +
                        "Current spending: $${String.format("%.2f", currentAmount)}\n" +
                        "Budget limit: $${String.format("%.2f", budgetLimit)}\n" +
                        "Percentage used: ${percentage}%"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(budgetPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        val notificationManager = ContextCompat.getSystemService(
            requireContext(),
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.notify(NOTIFICATION_ID + budgetType.hashCode() + 2000, notification)
        showBudgetLimitMessage(currentAmount, budgetLimit, budgetType)
    }

    private fun showBudgetLimitMessage(currentAmount: Double, budgetLimit: Double, budgetType: String) {
        val overAmount = currentAmount - budgetLimit
        val percentage = (currentAmount / budgetLimit * 100).toInt()

        val message = buildString {
            append("Budget Limit Message\n\n")
            append("Your $budgetType budget has been exceeded by $${String.format("%.2f", overAmount)}.\n\n")
            append("Current spending: $${String.format("%.2f", currentAmount)}\n")
            append("Budget limit: $${String.format("%.2f", budgetLimit)}\n")
            append("Percentage used: ${percentage}%\n\n")
            append("Would you like to review your budget?")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Budget Limit Message")
            .setMessage(message)
            .setPositiveButton("View Budget") { _, _ ->
                findNavController().navigate(R.id.nav_budget)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showApproachingBudgetNotification(currentAmount: Double, budgetLimit: Double, budgetType: String, percentage: Int) {
        // Check if we have notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Intent to open the Budget Fragment
        val budgetIntent = Intent(requireContext(), MainActivity::class.java).apply {
            action = "NAVIGATE_TO_BUDGET"
        }
        val budgetPendingIntent = PendingIntent.getActivity(
            requireContext(),
            1,
            budgetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to open the app
        val appIntent = requireActivity().packageManager.getLaunchIntentForPackage(requireActivity().packageName)
        val appPendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_warning_24)
            .setContentTitle("⚠️ Approaching Budget Limit")
            .setContentText("You've used ${percentage}% of your $budgetType budget")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've used ${percentage}% of your $budgetType budget\n\n" +
                        "Current spending: $${String.format("%.2f", currentAmount)}\n" +
                        "Budget limit: $${String.format("%.2f", budgetLimit)}\n\n" +
                        "Tap to view and manage your budget."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(appPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_baseline_settings_24, "View Budget", budgetPendingIntent)
            .setColor(ContextCompat.getColor(requireContext(), R.color.warning))
            .build()

        val notificationManager = ContextCompat.getSystemService(
            requireContext(),
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.notify(NOTIFICATION_ID + budgetType.hashCode() + 1000, notification)
    }

    private fun showDeleteConfirmationDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction(transaction)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        val transactions = adapter.transactionsList.toMutableList()
        transactions.remove(transaction)
        adapter.updateTransactions(transactions)
        updateBalance(transactions)
        saveData(transactions)
        showSnackbar("Transaction deleted")
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun loadSavedData() {
        val json = sharedPreferences.getString("transactions", null)
        val transactions = if (json != null) {
            gson.fromJson<List<Transaction>>(json, transactionType)
        } else {
            emptyList()
        }
        adapter.updateTransactions(transactions)
        updateBalance(transactions)
    }

    private fun saveData(transactions: List<Transaction>) {
        val json = gson.toJson(transactions)
        sharedPreferences.edit().putString("transactions", json).apply()
    }

    private fun updateBalance(transactions: List<Transaction>) {
        val balance = transactions.sumOf { transaction ->
            if (transaction.isIncome) transaction.amount else -transaction.amount
        }
        tvBalance.text = "$${String.format("%.2f", balance)}"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Budget Notifications"
            val descriptionText = "Notifications for budget limits"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Notifications are enabled
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show educational UI
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Notification Permission Required")
                        .setMessage("Budget notifications help you stay on track with your spending. Please enable notifications to receive budget alerts.")
                        .setPositiveButton("Enable") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Not Now", null)
                        .show()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}