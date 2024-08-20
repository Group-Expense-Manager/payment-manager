package pl.edu.agh.gem.internal.model.payment

import java.math.BigDecimal
import java.time.Instant

data class Payment(
    val id: String,
    val groupId: String,
    val creatorId: String,
    val recipientId: String,
    val title: String,
    val type: PaymentType,
    val amount: Amount,
    val fxData: FxData?,
    val date: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val attachmentId: String,
    val status: PaymentStatus,
    val history: List<PaymentHistoryEntry>,
)

data class Amount(
    val value: BigDecimal,
    val currency: String,
)

data class FxData(
    val targetCurrency: String,
    val exchangeRate: BigDecimal,
)
