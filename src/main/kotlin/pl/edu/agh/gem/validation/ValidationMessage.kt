package pl.edu.agh.gem.validation

object ValidationMessage {
    const val TITLE_NOT_BLANK = "Title can not be blank"
    const val TITLE_MAX_LENGTH = "Name must not exceed 30 characters"
    const val POSITIVE_SUM = "Sum must be positive"
    const val BASE_CURRENCY_NOT_BLANK = "Base currency can not be blank"
    const val BASE_CURRENCY_PATTERN = "Base currency must be a 3-letter uppercase code"
    const val TARGET_CURRENCY_PATTERN = "Target Currency must be null or a 3-letter uppercase code"
    const val ATTACHMENT_ID_NOT_BLANK = "AttachmentId can not be not blank"
    const val RECIPIENT_ID_NOT_BLANK = "Recipient's id can not be blank"
    const val MESSAGE_NULL_OR_NOT_BLANK = "Message can not be blank and not null at the same time"

    const val BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES = "Base currency must be in a group currencies"
    const val BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY = "Base currency must be different than target currency"
    const val TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES = "Target currency must be in a group currencies"
    const val BASE_CURRENCY_NOT_AVAILABLE = "Base currency is not available"

    const val RECIPIENT_IS_CREATOR = "Payment recipient can not be creator"
    const val RECIPIENT_NOT_GROUP_MEMBER = "Payment recipient is not a group member"
}
