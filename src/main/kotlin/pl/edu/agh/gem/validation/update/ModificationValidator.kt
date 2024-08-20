package pl.edu.agh.gem.validation.update

import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentUpdate
import pl.edu.agh.gem.validation.ValidationMessage.NO_MODIFICATION
import pl.edu.agh.gem.validator.BaseValidator
import pl.edu.agh.gem.validator.Check

class ModificationValidator : BaseValidator<PaymentUpdateDataWrapper>() {
    override val checks: List<Check<PaymentUpdateDataWrapper>> = listOf(
        Check(NO_MODIFICATION) { validateRecipient(it) },
    )

    private fun validateRecipient(updateDataWrapper: PaymentUpdateDataWrapper): Boolean {
        return updateDataWrapper.paymentUpdate.modifies(updateDataWrapper.originalPayment)
    }

    private fun PaymentUpdate.modifies(payment: Payment): Boolean {
        return payment.title != title ||
            payment.type != type ||
            payment.amount != amount ||
            payment.fxData?.targetCurrency != targetCurrency ||
            payment.date != date
    }
}
