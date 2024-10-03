package pl.edu.agh.gem.external.dto.payment

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import pl.edu.agh.gem.annotation.nullorblank.NullOrNotBlank
import pl.edu.agh.gem.annotation.nullorpattern.NullOrPattern
import pl.edu.agh.gem.internal.model.payment.PaymentType
import pl.edu.agh.gem.internal.model.payment.PaymentUpdate
import pl.edu.agh.gem.validation.ValidationMessage.ATTACHMENT_ID_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.MESSAGE_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_MAX_LENGTH
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_NOT_BLANK
import java.time.Instant

data class PaymentUpdateRequest(
    @field:NotBlank(message = TITLE_NOT_BLANK)
    @field:Size(max = 30, message = TITLE_MAX_LENGTH)
    val title: String,
    val type: PaymentType,
    @field:Valid
    val amount: AmountDto,
    @field:NullOrPattern(message = TARGET_CURRENCY_PATTERN, pattern = "[A-Z]{3}")
    val targetCurrency: String? = null,
    @field:DateTimeFormat(iso = DATE_TIME)
    val date: Instant,
    @field:NullOrNotBlank(message = MESSAGE_NULL_OR_NOT_BLANK)
    val message: String? = null,
    @field:NullOrNotBlank(message = ATTACHMENT_ID_NULL_OR_NOT_BLANK)
    val attachmentId: String?,
) {
    fun toDomain(paymentId: String, groupId: String, userId: String) = PaymentUpdate(
        id = paymentId,
        groupId = groupId,
        userId = userId,
        title = title,
        type = type,
        amount = amount.toDomain(),
        targetCurrency = targetCurrency,
        date = date,
        message = message,
        attachmentId = attachmentId,
    )
}
