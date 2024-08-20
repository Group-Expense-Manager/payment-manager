package pl.edu.agh.gem.validation.creation

import pl.edu.agh.gem.internal.model.currency.Currency
import pl.edu.agh.gem.internal.model.group.GroupData
import pl.edu.agh.gem.internal.model.payment.PaymentCreation
import pl.edu.agh.gem.validator.DataWrapper

data class PaymentCreationDataWrapper(
    val groupData: GroupData,
    val paymentCreation: PaymentCreation,
    val availableCurrencies: List<Currency>,
) : DataWrapper
