package ru.iu3.application.usecase

import ru.iu3.domain.model.Order
import ru.iu3.domain.model.PaymentMethod

internal interface CheckoutUseCase {

    operator fun invoke(userId: String, method: PaymentMethod): Order
}