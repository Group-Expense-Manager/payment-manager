package pl.edu.agh.gem.external.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import pl.edu.agh.gem.internal.model.payment.Amount
import pl.edu.agh.gem.internal.model.payment.FxData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentHistoryEntry
import pl.edu.agh.gem.internal.model.payment.PaymentStatus
import pl.edu.agh.gem.internal.model.payment.PaymentType
import java.time.Instant

@Document("payments")
data class PaymentEntity(
    @Id
    val id: String,
    val groupId: String,
    val creatorId: String,
    val recipientId: String,
    @Indexed(background = true)
    val title: String,
    val type: PaymentType,
    val amount: Amount,
    val fxData: FxData?,
    @Indexed(background = true)
    val date: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val attachmentId: String,
    val status: PaymentStatus,
    val history: List<PaymentHistoryEntry>,
) {
    fun toDomain() = Payment(
        id = id,
        groupId = groupId,
        creatorId = creatorId,
        recipientId = recipientId,
        title = title,
        type = type,
        amount = amount,
        fxData = fxData,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
        attachmentId = attachmentId,
        status = status,
        history = history,
    )
}

fun Payment.toEntity() = PaymentEntity(
    id = id,
    groupId = groupId,
    creatorId = creatorId,
    recipientId = recipientId,
    title = title,
    type = type,
    amount = amount,
    fxData = fxData,
    date = date,
    createdAt = createdAt,
    updatedAt = updatedAt,
    attachmentId = attachmentId,
    status = status,
    history = history,
)
