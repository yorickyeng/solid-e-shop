package ru.iu3.domain.payment

import ru.iu3.domain.model.OrderStatus
import ru.iu3.domain.model.PaymentMethod

internal interface PaymentStrategy {
    val method: PaymentMethod
    fun pay(price: Double): OrderStatus
}