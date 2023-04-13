package com.semih.mcdroid

import java.util.*

data class InventoryItem(
    val invid: Int,
    val invname: String,
    val depname: String,
    val invdate: Date
) {
    var checked: Boolean = false
}