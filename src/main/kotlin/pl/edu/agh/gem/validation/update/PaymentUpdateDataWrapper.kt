package pl.edu.agh.gem.validation.update

import pl.edu.agh.gem.validation.CurrencyData
import pl.edu.agh.gem.validation.CurrencyDataWrapper

data class PaymentUpdateDataWrapper(
    override val currencyData: CurrencyData,
) : CurrencyDataWrapper
