package pl.edu.agh.gem.external.dto.payment

import pl.edu.agh.gem.internal.model.payment.FxData
import pl.edu.agh.gem.internal.model.payment.Payment
import java.math.BigDecimal
import java.time.Instant

data class AcceptedGroupPaymentsResponse(
    val groupId: String,
    val payments: List<AcceptedGroupPaymentDto>,
)

data class AcceptedGroupPaymentDto(
    val creatorId: String,
    val recipientId: String,
    val title: String,
    val amount: AmountDto,
    val fxData: FxDataDto?,
    val date: Instant,
)

data class FxDataDto(
    val targetCurrency: String,
    val exchangeRate: BigDecimal,
)

fun Payment.toAcceptedGroupPaymentDto() = AcceptedGroupPaymentDto(
    creatorId = creatorId,
    recipientId = recipientId,
    title = title,
    amount = amount.toAmountDto(),
    fxData = fxData?.toDto(),
    date = date,
)

fun FxData.toDto() = FxDataDto(
    targetCurrency = targetCurrency,
    exchangeRate = exchangeRate,

)

fun List<Payment>.toAcceptedGroupPaymentsResponse(groupId: String) = AcceptedGroupPaymentsResponse(
    groupId = groupId,
    payments = map { it.toAcceptedGroupPaymentDto() },
)
