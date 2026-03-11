package ru.iu3.domain.payment

import ru.iu3.domain.model.OrderStatus
import ru.iu3.domain.model.PaymentMethod

internal interface PaymentGateway {

    fun pay(method: PaymentMethod, price: Double): OrderStatus
}