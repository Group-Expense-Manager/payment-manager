package pl.edu.agh.gem.external.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import pl.edu.agh.gem.internal.model.payment.Amount
import pl.edu.agh.gem.internal.model.payment.FxData
import pl.edu.agh.gem.internal.model.payment.PaymentHistoryEntry
import pl.edu.agh.gem.internal.model.payment.PaymentStatus
import pl.edu.agh.gem.internal.model.payment.PaymentType
import java.time.Instant

@Document("archived-payments")
data class ArchivedPaymentEntity(
    @Id
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
    val attachmentId: String?,
    val status: PaymentStatus,
    val history: List<PaymentHistoryEntry>,
)
