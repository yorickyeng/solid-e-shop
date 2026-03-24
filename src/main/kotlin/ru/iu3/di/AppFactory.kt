package ru.iu3.di

import ru.iu3.application.generator.IdGenerator
import ru.iu3.application.usecase.impl.*
import ru.iu3.domain.repository.CartRepository
import ru.iu3.domain.repository.OrderRepository
import ru.iu3.domain.repository.ProductRepository
import ru.iu3.infrastructure.generator.UuidGenerator
import ru.iu3.infrastructure.payment.BonusPaymentStrategy
import ru.iu3.infrastructure.payment.CardPaymentStrategy
import ru.iu3.infrastructure.payment.CashPaymentStrategy
import ru.iu3.infrastructure.repository.CartRepositoryImpl
import ru.iu3.infrastructure.repository.OrderRepositoryImpl
import ru.iu3.infrastructure.repository.ProductRepositoryImpl
import ru.iu3.infrastructure.seed.StaticProducts
import ru.iu3.presentation.console.ConsoleDependencies

internal object AppFactory {

    fun createConsoleDeps(): ConsoleDependencies {
        val products = StaticProducts.load()

        val productRepository: ProductRepository = ProductRepositoryImpl(products)
        val cartRepository: CartRepository = CartRepositoryImpl()
        val orderRepository: OrderRepository = OrderRepositoryImpl()
        val idGenerator: IdGenerator = UuidGenerator()
        val paymentStrategies = listOf(
            CardPaymentStrategy(),
            CashPaymentStrategy(),
            BonusPaymentStrategy(),
        )

        return ConsoleDependencies(
            getAllProducts = GetAllProductsUseCaseImpl(productRepository),
            getFilteredProducts = GetFilteredProductsUseCaseImpl(productRepository),
            addToCart = AddProductToCartUseCaseImpl(productRepository, cartRepository),
            removeFromCart = RemoveProductFromCartUseCaseImpl(productRepository, cartRepository),
            getCart = GetCartUseCaseImpl(cartRepository),
            clearCart = ClearCartUseCaseImpl(cartRepository),
            checkout = CheckoutUseCaseImpl(cartRepository, orderRepository, idGenerator),
            getOrderHistory = GetOrderHistoryUseCaseImpl(orderRepository),
            paymentStrategies = paymentStrategies,
        )
    }
}