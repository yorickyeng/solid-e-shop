package ru.iu3.infrastructure.payment

import ru.iu3.domain.exception.InvalidAmountException
import ru.iu3.domain.model.OrderStatus
import ru.iu3.domain.model.PaymentMethod
import ru.iu3.domain.payment.PaymentStrategy

internal class CardPaymentStrategy : PaymentStrategy {
    override val method: PaymentMethod = PaymentMethod.CARD

    override fun pay(price: Double): OrderStatus {
        if (price <= 0) throw InvalidAmountException()
        return OrderStatus.SUCCESS
    }
}

internal class CashPaymentStrategy : PaymentStrategy {
    override val method: PaymentMethod = PaymentMethod.CASH

    override fun pay(price: Double): OrderStatus {
        if (price <= 0) throw InvalidAmountException()
        return OrderStatus.SUCCESS
    }
}

internal class BonusPaymentStrategy(
    private val availableBonuses: Double
) : PaymentStrategy {
    override val method: PaymentMethod = PaymentMethod.BONUS

    override fun pay(price: Double): OrderStatus {
        if (price <= 0) throw InvalidAmountException()
        if (price > availableBonuses) return OrderStatus.ERROR
        return OrderStatus.SUCCESS
    }
}