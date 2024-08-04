package pl.edu.agh.gem.internal.service

import org.springframework.stereotype.Service
import pl.edu.agh.gem.internal.client.CurrencyManagerClient
import pl.edu.agh.gem.internal.client.GroupManagerClient
import pl.edu.agh.gem.internal.model.group.GroupData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.persistence.PaymentRepository
import pl.edu.agh.gem.validation.creation.CurrenciesValidator
import pl.edu.agh.gem.validation.creation.PaymentCreationDataWrapper
import pl.edu.agh.gem.validation.creation.RecipientValidator
import pl.edu.agh.gem.validator.ValidatorList.Companion.validatorsOf
import pl.edu.agh.gem.validator.ValidatorsException
import java.time.Instant

@Service
class PaymentService(
    private val groupManagerClient: GroupManagerClient,
    private val currencyManagerClient: CurrencyManagerClient,
    private val paymentRepository: PaymentRepository,
) {

    private val expenseCreationValidators = validatorsOf(
        RecipientValidator(),
        CurrenciesValidator(),
    )

    fun getGroup(groupId: String): GroupData {
        return groupManagerClient.getGroup(groupId)
    }

    fun createPayment(groupData: GroupData, payment: Payment): Payment {
        expenseCreationValidators
            .getFailedValidations(createExpenseCreationDataWrapper(groupData, payment))
            .takeIf { it.isNotEmpty() }
            ?.also { throw ValidatorsException(it) }

        return paymentRepository.save(
            payment.copy(
                exchangeRate = getExchangeRate(payment.baseCurrency, payment.targetCurrency, payment.createdAt),
            ),
        )
    }

    private fun createExpenseCreationDataWrapper(groupData: GroupData, payment: Payment): PaymentCreationDataWrapper {
        return PaymentCreationDataWrapper(
            groupData,
            payment,
            currencyManagerClient.getAvailableCurrencies(),
        )
    }

    private fun getExchangeRate(baseCurrency: String, targetCurrency: String?, date: Instant) =
        targetCurrency?.let { currencyManagerClient.getExchangeRate(baseCurrency, targetCurrency, date) }?.value
}
