package com.example.finessa.model

import java.io.Serializable
import java.util.Date

data class Transaction(
    val id: Int,
    val title: String,
    val amount: Double,
    val category: String,
    val date: Date,
    val isIncome: Boolean,
    val description: String = "",
    val recurring: Boolean = false
) : Serializable