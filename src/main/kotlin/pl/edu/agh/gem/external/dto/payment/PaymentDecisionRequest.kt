package pl.edu.agh.gem.external.dto.payment

import jakarta.validation.constraints.NotBlank
import pl.edu.agh.gem.annotation.nullorblank.NullOrNotBlank
import pl.edu.agh.gem.internal.model.payment.Decision
import pl.edu.agh.gem.internal.model.payment.PaymentDecision
import pl.edu.agh.gem.validation.ValidationMessage.GROUP_ID_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.MESSAGE_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.PAYMENT_ID_NOT_BLANK

data class PaymentDecisionRequest(
    @field:NotBlank(message = PAYMENT_ID_NOT_BLANK)
    val paymentId: String,
    @field:NotBlank(message = GROUP_ID_NOT_BLANK)
    val groupId: String,
    val decision: Decision,
    @field:NullOrNotBlank(message = MESSAGE_NULL_OR_NOT_BLANK)
    val message: String?,
) {
    fun toDomain(userId: String) = PaymentDecision(
        userId = userId,
        paymentId = paymentId,
        groupId = groupId,
        decision = decision,
        message = message,
    )
}
