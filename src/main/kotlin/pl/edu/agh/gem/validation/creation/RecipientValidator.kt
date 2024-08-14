package pl.edu.agh.gem.validation.creation

import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_IS_CREATOR
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_NOT_GROUP_MEMBER
import pl.edu.agh.gem.validator.BaseValidator
import pl.edu.agh.gem.validator.Check

class RecipientValidator : BaseValidator<PaymentCreationDataWrapper>() {
    override val checks: List<Check<PaymentCreationDataWrapper>> = listOf(
        Check(RECIPIENT_IS_CREATOR) { this.validateIfUserIsNotRecipient(it) },
        Check(RECIPIENT_NOT_GROUP_MEMBER) { this.validateIfRecipientIsGroupMember(it) },
    )

    private fun validateIfUserIsNotRecipient(paymentCreationDataWrapper: PaymentCreationDataWrapper): Boolean {
        return paymentCreationDataWrapper.paymentCreation.creatorId != paymentCreationDataWrapper.paymentCreation.recipientId
    }

    private fun validateIfRecipientIsGroupMember(paymentCreationDataWrapper: PaymentCreationDataWrapper): Boolean {
        val membersIds = paymentCreationDataWrapper.groupMembers.members.map { it.id }
        return paymentCreationDataWrapper.paymentCreation.recipientId in membersIds
    }
}
