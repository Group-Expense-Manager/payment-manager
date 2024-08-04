package pl.edu.agh.gem.external.dto.payment

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import pl.edu.agh.gem.annotation.nullorblank.NullOrNotBlank
import pl.edu.agh.gem.annotation.nullorpattern.NullOrPattern
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentAction.CREATED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.model.payment.PaymentType
import pl.edu.agh.gem.internal.model.payment.StatusHistoryEntry
import pl.edu.agh.gem.validation.ValidationMessage.ATTACHMENT_ID_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.MESSAGE_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.POSITIVE_SUM
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_ID_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_MAX_LENGTH
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_NOT_BLANK
import java.math.BigDecimal
import java.time.Instant.now
import java.util.UUID.randomUUID

data class PaymentCreationRequest(
    @field:NotBlank(message = TITLE_NOT_BLANK)
    @field:Size(max = 30, message = TITLE_MAX_LENGTH)
    val title: String,
    val type: PaymentType,
    @field:Positive(message = POSITIVE_SUM)
    val sum: BigDecimal,
    @field:NotBlank(message = BASE_CURRENCY_NOT_BLANK)
    @field:Pattern(regexp = "[A-Z]{3}", message = BASE_CURRENCY_PATTERN)
    val baseCurrency: String,
    @field:NullOrPattern(message = TARGET_CURRENCY_PATTERN, pattern = "[A-Z]{3}")
    val targetCurrency: String?,
    @field:NotBlank(message = RECIPIENT_ID_NOT_BLANK)
    val recipientId: String,
    @field:NullOrNotBlank(message = MESSAGE_NULL_OR_NOT_BLANK)
    val message: String? = null,
    @field:NotBlank(message = ATTACHMENT_ID_NOT_BLANK)
    val attachmentId: String,
) {
    fun toDomain(userId: String, groupId: String) = Payment(
        id = randomUUID().toString(),
        groupId = groupId,
        creatorId = userId,
        recipientId = recipientId,
        title = title,
        type = type,
        sum = sum,
        baseCurrency = baseCurrency,
        targetCurrency = targetCurrency,
        exchangeRate = null,
        createdAt = now(),
        updatedAt = now(),
        attachmentId = attachmentId,
        status = PENDING,
        statusHistory = arrayListOf(StatusHistoryEntry(userId, CREATED, comment = message)),

    )
}
