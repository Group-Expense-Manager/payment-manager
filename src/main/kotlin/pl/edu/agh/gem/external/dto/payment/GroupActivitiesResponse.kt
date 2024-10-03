package pl.edu.agh.gem.external.dto.payment

import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentStatus
import java.time.Instant

data class GroupActivitiesResponse(
    val groupId: String,
    val payments: List<GroupActivityDTO>,
)

data class GroupActivityDTO(
    val paymentId: String,
    val creatorId: String,
    val recipientId: String,
    val title: String,
    val amount: AmountDto,
    val fxData: FxDataDto?,
    val status: PaymentStatus,
    val date: Instant,
) {
    companion object {
        fun fromPayment(payment: Payment) = GroupActivityDTO(
            paymentId = payment.id,
            creatorId = payment.creatorId,
            recipientId = payment.recipientId,
            title = payment.title,
            amount = payment.amount.toAmountDto(),
            fxData = payment.fxData?.toDto(),
            status = payment.status,
            date = payment.date,
        )
    }
}

fun List<Payment>.toGroupActivitiesResponse(groupId: String) = GroupActivitiesResponse(
    groupId = groupId,
    payments = map { GroupActivityDTO.fromPayment(it) },
)
