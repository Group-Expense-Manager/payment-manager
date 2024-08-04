package pl.edu.agh.gem.external.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentStatus
import pl.edu.agh.gem.internal.model.payment.PaymentType
import pl.edu.agh.gem.internal.model.payment.StatusHistoryEntry
import java.math.BigDecimal
import java.time.Instant

@Document("payments")
data class PaymentEntity(
    @Id
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
) {
    fun toDomain() = Payment(
        id = id,
        groupId = groupId,
        creatorId = creatorId,
        recipientId = recipientId,
        title = title,
        type = type,
        sum = sum,
        baseCurrency = baseCurrency,
        targetCurrency = targetCurrency,
        exchangeRate = exchangeRate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        attachmentId = attachmentId,
        status = status,
        statusHistory = statusHistory,
    )
}

fun Payment.toEntity() = PaymentEntity(
    id = id,
    groupId = groupId,
    creatorId = creatorId,
    recipientId = recipientId,
    title = title,
    type = type,
    sum = sum,
    baseCurrency = baseCurrency,
    targetCurrency = targetCurrency,
    exchangeRate = exchangeRate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    attachmentId = attachmentId,
    status = status,
    statusHistory = statusHistory,
)
