package pl.edu.agh.gem.validation.update

import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentUpdate
import pl.edu.agh.gem.validation.CreatorData
import pl.edu.agh.gem.validation.CreatorDataWrapper
import pl.edu.agh.gem.validation.CurrencyData
import pl.edu.agh.gem.validation.CurrencyDataWrapper

data class PaymentUpdateDataWrapper(
    val originalPayment: Payment,
    val paymentUpdate: PaymentUpdate,
    override val currencyData: CurrencyData,
    override val creatorData: CreatorData,
) : CurrencyDataWrapper, CreatorDataWrapper
