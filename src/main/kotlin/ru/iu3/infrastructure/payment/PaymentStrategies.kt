package ru.iu3.infrastructure.payment

import ru.iu3.domain.exception.InsufficientBonusesException
import ru.iu3.domain.exception.InvalidAmountException
import ru.iu3.domain.model.OrderStatus
import ru.iu3.domain.payment.PaymentStrategy

internal class CardPaymentStrategy : PaymentStrategy {

    override val name: String = "Карта"

    override fun pay(price: Double): OrderStatus {
        if (price <= 0) throw InvalidAmountException()
        return OrderStatus.SUCCESS
    }
}

internal class CashPaymentStrategy : PaymentStrategy {

    override val name: String = "Наличные"

    override fun pay(price: Double): OrderStatus {
        if (price <= 0) throw InvalidAmountException()
        return OrderStatus.SUCCESS
    }
}

internal class BonusPaymentStrategy(
    private val availableBonuses: Double = 50000.0,
) : PaymentStrategy {

    override val name: String = "Бонусы"

    override fun pay(price: Double): OrderStatus {
        if (price <= 0) throw InvalidAmountException()
        if (price > availableBonuses) throw InsufficientBonusesException()
        return OrderStatus.SUCCESS
    }
}