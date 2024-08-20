package pl.edu.agh.gem.internal.mapper

import pl.edu.agh.gem.internal.model.payment.BalanceElement
import pl.edu.agh.gem.internal.model.payment.Payment
import java.math.BigDecimal

class BalanceElementMapper {

    fun mapToBalanceElement(userId: String, payment: Payment): BalanceElement? =
        when {
            userId.isPaymentParticipant(payment) -> BalanceElement(
                value = getSignedValue(payment, userId),
                currency = payment.fxData?.targetCurrency ?: payment.amount.currency,
                exchangeRate = payment.fxData?.exchangeRate,
            )
            else -> null
        }

    private fun String.isPaymentParticipant(payment: Payment) =
        setOf(payment.creatorId, payment.recipientId).contains(this)

    private fun getSignedValue(payment: Payment, userId: String): BigDecimal {
        return if (userId == payment.creatorId) payment.amount.value else payment.amount.value.negate()
    }
}
