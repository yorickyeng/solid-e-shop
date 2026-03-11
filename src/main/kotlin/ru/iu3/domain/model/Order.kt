package ru.iu3.domain.model

internal data class Order(
    val id: String,
    val userId: String,
    val status: OrderStatus,
    val totalPrice: Double,
    val paymentMethod: PaymentMethod,
    val items: List<CartItem>,
)