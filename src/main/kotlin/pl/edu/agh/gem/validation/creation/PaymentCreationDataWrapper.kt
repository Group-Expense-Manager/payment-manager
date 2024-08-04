package pl.edu.agh.gem.validation.creation

import pl.edu.agh.gem.internal.model.group.Currencies
import pl.edu.agh.gem.internal.model.group.GroupData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.validator.DataWrapper

data class PaymentCreationDataWrapper(
    val groupData: GroupData,
    val payment: Payment,
    val availableCurrencies: Currencies,
) : DataWrapper
