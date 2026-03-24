package ru.iu3.application.usecase.impl

import ru.iu3.application.generator.IdGenerator
import ru.iu3.application.usecase.CheckoutUseCase
import ru.iu3.domain.exception.EmptyCartException
import ru.iu3.domain.model.Order
import ru.iu3.domain.payment.PaymentStrategy
import ru.iu3.domain.repository.CartRepository
import ru.iu3.domain.repository.OrderRepository

internal class CheckoutUseCaseImpl(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val idGenerator: IdGenerator,
) : CheckoutUseCase {

    override fun invoke(userId: String, strategy: PaymentStrategy): Order {
        val cart = cartRepository.get()
        if (cart.isEmpty()) throw EmptyCartException()

        val items = cart.getItems()
        val totalPrice = cart.getTotalPrice()

        val status = strategy.pay(totalPrice)

        val order = Order(
            id = idGenerator.newId(),
            userId = userId,
            status = status,
            totalPrice = totalPrice,
            paymentMethod = strategy.method,
            items = items,
        )

        orderRepository.save(order)
        cartRepository.clear()

        return order
    }
}