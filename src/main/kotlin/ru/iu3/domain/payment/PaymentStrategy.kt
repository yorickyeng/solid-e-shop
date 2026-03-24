package ru.iu3.domain.payment

import ru.iu3.domain.model.OrderStatus

internal interface PaymentStrategy {

    val name: String

    fun pay(price: Double): OrderStatus
}