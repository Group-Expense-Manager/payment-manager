package pl.edu.agh.gem.validation.creation

import pl.edu.agh.gem.internal.model.payment.PaymentCreation
import pl.edu.agh.gem.model.GroupMembers
import pl.edu.agh.gem.validation.CurrencyData
import pl.edu.agh.gem.validation.CurrencyDataWrapper

data class PaymentCreationDataWrapper(
    val groupMembers: GroupMembers,
    val paymentCreation: PaymentCreation,
    override val currencyData: CurrencyData,
) : CurrencyDataWrapper
