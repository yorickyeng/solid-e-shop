package ru.iu3.infrastructure.payment

import ru.iu3.domain.exception.InvalidAmountException
import ru.iu3.domain.model.OrderStatus
import ru.iu3.domain.model.PaymentMethod
import ru.iu3.domain.payment.PaymentGateway

internal class PaymentGatewayImpl : PaymentGateway {

    override fun pay(method: PaymentMethod, price: Double): OrderStatus {
        if (price <= 0) throw InvalidAmountException()
        return OrderStatus.SUCCESS
    }
}