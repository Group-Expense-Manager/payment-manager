package pl.edu.agh.gem.validation

import pl.edu.agh.gem.validator.DataWrapper

interface CurrencyDataWrapper : DataWrapper {
    val currencyData: CurrencyData
}
