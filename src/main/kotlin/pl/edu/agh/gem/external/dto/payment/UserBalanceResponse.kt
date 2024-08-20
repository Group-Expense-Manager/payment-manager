package pl.edu.agh.gem.external.dto.payment

import pl.edu.agh.gem.internal.model.payment.BalanceElement
import java.math.BigDecimal

data class UserBalanceResponse(
    val userId: String,
    val elements: List<BalanceElementDto>,
)

data class BalanceElementDto(
    val value: BigDecimal,
    val currency: String,
    val exchangeRate: BigDecimal?,

)

private fun BalanceElement.toDto() = BalanceElementDto(
    value = value,
    currency = currency,
    exchangeRate = exchangeRate,
)

fun List<BalanceElement>.toUserBalanceResponse(userId: String) = UserBalanceResponse(
    userId = userId,
    elements = map { it.toDto() },
)
