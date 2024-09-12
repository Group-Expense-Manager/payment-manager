package pl.edu.agh.gem.external.dto.payment

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import pl.edu.agh.gem.annotation.nullorblank.NullOrNotBlank
import pl.edu.agh.gem.annotation.nullorpattern.NullOrPattern
import pl.edu.agh.gem.internal.model.payment.Amount
import pl.edu.agh.gem.internal.model.payment.PaymentCreation
import pl.edu.agh.gem.internal.model.payment.PaymentType
import pl.edu.agh.gem.validation.ValidationMessage.ATTACHMENT_ID_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.MESSAGE_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.POSITIVE_AMOUNT
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_ID_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_MAX_LENGTH
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_NOT_BLANK
import java.math.BigDecimal
import java.time.Instant

data class PaymentCreationRequest(
    @field:NotBlank(message = TITLE_NOT_BLANK)
    @field:Size(max = 30, message = TITLE_MAX_LENGTH)
    val title: String,
    val type: PaymentType,
    @field:Valid
    val amount: AmountDto,
    @field:NullOrPattern(message = TARGET_CURRENCY_PATTERN, pattern = "[A-Z]{3}")
    val targetCurrency: String?,
    @field:DateTimeFormat(iso = DATE_TIME)
    val date: Instant,
    @field:NotBlank(message = RECIPIENT_ID_NOT_BLANK)
    val recipientId: String,
    @field:NullOrNotBlank(message = MESSAGE_NULL_OR_NOT_BLANK)
    val message: String? = null,
    @field:NullOrNotBlank(message = ATTACHMENT_ID_NULL_OR_NOT_BLANK)
    val attachmentId: String?,
) {
    fun toDomain(userId: String, groupId: String) = PaymentCreation(
        groupId = groupId,
        creatorId = userId,
        recipientId = recipientId,
        title = title,
        type = type,
        amount = amount.toDomain(),
        targetCurrency = targetCurrency,
        date = date,
        message = message,
        attachmentId = attachmentId,
    )
}

data class AmountDto(
    @field:Positive(message = POSITIVE_AMOUNT)
    val value: BigDecimal,
    @field:NotBlank(message = BASE_CURRENCY_NOT_BLANK)
    @field:Pattern(regexp = "[A-Z]{3}", message = BASE_CURRENCY_PATTERN)
    val currency: String,
) {
    fun toDomain() = Amount(
        value = value,
        currency = currency,
    )
}
