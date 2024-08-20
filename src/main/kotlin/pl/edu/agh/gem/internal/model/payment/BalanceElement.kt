package pl.edu.agh.gem.internal.model.payment

import java.math.BigDecimal

data class BalanceElement(
    val value: BigDecimal,
    val currency: String,
    val exchangeRate: BigDecimal?,
)
