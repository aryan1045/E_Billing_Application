package com.example.e_billing

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductParcelable(
    val name: String,
    val quantity: Int,
    val price: Double,
    val total: Double
) : Parcelable
