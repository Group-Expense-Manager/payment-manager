package pl.edu.agh.gem.internal.model.payment

import pl.edu.agh.gem.internal.model.payment.PaymentAction.CREATED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import java.time.Instant
import java.time.Instant.now
import java.util.UUID.randomUUID

data class PaymentCreation(
    val groupId: String,
    val creatorId: String,
    val recipientId: String,
    val title: String,
    val type: PaymentType,
    val amount: Amount,
    val targetCurrency: String?,
    val date: Instant,
    val message: String? = null,
    val attachmentId: String?,
) {
    fun toPayment(fxData: FxData?, attachmentId: String) = Payment(
        id = randomUUID().toString(),
        groupId = groupId,
        creatorId = creatorId,
        recipientId = recipientId,
        title = title,
        type = type,
        amount = amount,
        fxData = fxData,
        date = date,
        createdAt = now(),
        updatedAt = now(),
        attachmentId = attachmentId,
        status = PENDING,
        history = arrayListOf(PaymentHistoryEntry(creatorId, CREATED, comment = message)),
    )
}
