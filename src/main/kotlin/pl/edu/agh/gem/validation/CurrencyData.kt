package pl.edu.agh.gem.validation

import pl.edu.agh.gem.internal.model.currency.Currency

data class CurrencyData(
    val groupCurrencies: List<Currency>,
    val availableCurrencies: List<Currency>,
    val baseCurrency: String,
    val targetCurrency: String?,
)
