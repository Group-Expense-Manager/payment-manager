package pl.edu.agh.gem.external.dto.payment

import pl.edu.agh.gem.internal.model.payment.Payment

data class PaymentUpdateResponse(
    val paymentId: String,
)
fun Payment.toPaymentUpdateResponse() = PaymentUpdateResponse(id)
