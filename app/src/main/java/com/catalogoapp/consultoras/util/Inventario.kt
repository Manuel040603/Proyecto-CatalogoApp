package com.catalogoapp.consultoras.util

object Inventario {

    const val STOCK_BAJO = 5

    fun esBajo(stock: Int): Boolean = stock in 1..STOCK_BAJO

    fun agotado(stock: Int): Boolean = stock <= 0

    fun etiqueta(stock: Int): String? {
        return when {
            agotado(stock) -> "Agotado"
            esBajo(stock) -> "Quedan $stock"
            else -> null
        }
    }
}
