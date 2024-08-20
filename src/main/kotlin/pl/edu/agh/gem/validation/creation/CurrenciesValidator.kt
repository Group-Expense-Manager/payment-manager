package pl.edu.agh.gem.validation.creation

import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_AVAILABLE
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES
import pl.edu.agh.gem.validator.BaseValidator
import pl.edu.agh.gem.validator.Check

class CurrenciesValidator : BaseValidator<PaymentCreationDataWrapper>() {
    override val checks: List<Check<PaymentCreationDataWrapper>> = listOf(
        Check(BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES) { validateBaseCurrencyInGroupCurrencies(it) },
        Check(BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY) { validateBaseCurrencyNotEqualTargetCurrency(it) },
        Check(TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES) { validateTargetCurrencyInGroupCurrencies(it) },
        Check(BASE_CURRENCY_NOT_AVAILABLE) { validateBaseCurrencyAvailable(it) },
    )

    private fun validateBaseCurrencyInGroupCurrencies(paymentCreationDataWrapper: PaymentCreationDataWrapper): Boolean {
        return paymentCreationDataWrapper.paymentCreation.targetCurrency != null ||
            paymentCreationDataWrapper.groupData.currencies.any { it.code == paymentCreationDataWrapper.paymentCreation.amount.currency }
    }

    private fun validateBaseCurrencyNotEqualTargetCurrency(paymentCreationDataWrapper: PaymentCreationDataWrapper): Boolean {
        return paymentCreationDataWrapper.paymentCreation.targetCurrency == null ||
            paymentCreationDataWrapper.paymentCreation.amount.currency != paymentCreationDataWrapper.paymentCreation.targetCurrency
    }

    private fun validateTargetCurrencyInGroupCurrencies(paymentCreationDataWrapper: PaymentCreationDataWrapper): Boolean {
        return paymentCreationDataWrapper.paymentCreation.targetCurrency == null ||
            paymentCreationDataWrapper.groupData.currencies.any { it.code == paymentCreationDataWrapper.paymentCreation.targetCurrency }
    }

    private fun validateBaseCurrencyAvailable(paymentCreationDataWrapper: PaymentCreationDataWrapper): Boolean {
        return paymentCreationDataWrapper.paymentCreation.targetCurrency == null ||
            paymentCreationDataWrapper.availableCurrencies.any { it.code == paymentCreationDataWrapper.paymentCreation.amount.currency }
    }
}
