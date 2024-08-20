package pl.edu.agh.gem.validation.decision

import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentDecision
import pl.edu.agh.gem.validator.DataWrapper

data class DecisionDataWrapper(
    val paymentDecision: PaymentDecision,
    val payment: Payment,
) : DataWrapper
