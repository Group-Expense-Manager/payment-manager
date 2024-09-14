package pl.edu.agh.gem.external.dto.payment

import pl.edu.agh.gem.internal.model.payment.Amount
import pl.edu.agh.gem.internal.model.payment.FxData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentHistoryEntry
import java.time.Instant

data class PaymentResponse(
    val paymentId: String,
    val creatorId: String,
    val recipientId: String,
    val title: String,
    val type: String,
    val amount: AmountDto,
    val fxData: FxData?,
    val date: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val attachmentId: String,
    val status: String,
    val history: List<PaymentHistoryDto>,
)

fun Payment.toPaymentResponse() = PaymentResponse(
    paymentId = id,
    creatorId = creatorId,
    recipientId = recipientId,
    title = title,
    type = type.name,
    amount = amount.toAmountDto(),
    fxData = fxData,
    date = date,
    createdAt = createdAt,
    updatedAt = updatedAt,
    attachmentId = attachmentId,
    status = status.name,
    history = history.map { it.toDto() },
)

data class PaymentHistoryDto(
    val participantId: String,
    val paymentAction: String,
    val createdAt: Instant,
    val comment: String?,
)

fun PaymentHistoryEntry.toDto() = PaymentHistoryDto(
    participantId = participantId,
    paymentAction = paymentAction.name,
    createdAt = createdAt,
    comment = comment,
)

fun Amount.toAmountDto() = AmountDto(value, currency)
