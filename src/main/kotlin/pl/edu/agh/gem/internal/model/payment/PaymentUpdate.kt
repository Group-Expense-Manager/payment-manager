package pl.edu.agh.gem.internal.model.payment

import java.time.Instant

data class PaymentUpdate(
    val id: String,
    val groupId: String,
    val userId: String,
    val title: String,
    val type: PaymentType,
    val amount: Amount,
    val targetCurrency: String?,
    val date: Instant,
    val message: String?,
)
