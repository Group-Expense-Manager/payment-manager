package pl.edu.agh.gem.validation.decision

import pl.edu.agh.gem.validation.ValidationMessage.USER_NOT_RECIPIENT
import pl.edu.agh.gem.validator.BaseValidator
import pl.edu.agh.gem.validator.Check

class DecisionValidator : BaseValidator<PaymentDecisionDataWrapper>() {
    override val checks: List<Check<PaymentDecisionDataWrapper>> = listOf(
        Check(USER_NOT_RECIPIENT) { validateRecipient(it) },
    )

    private fun validateRecipient(decisionDataWrapper: PaymentDecisionDataWrapper): Boolean {
        return decisionDataWrapper.paymentDecision.userId == decisionDataWrapper.payment.recipientId
    }
}
