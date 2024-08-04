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
    val sum: BigDecimal,
    val baseCurrency: String,
    val targetCurrency: String?,
    val exchangeRate: BigDecimal?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val attachmentId: String,
    val status: PaymentStatus,
    val statusHistory: List<StatusHistoryEntry>,
)
