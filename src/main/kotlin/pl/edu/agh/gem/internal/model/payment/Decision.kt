package pl.edu.agh.gem.internal.model.payment

enum class Decision {
    ACCEPT,
    REJECT,
    ;

    fun toPaymentStatus(): PaymentStatus {
        return when (this) {
            ACCEPT -> PaymentStatus.ACCEPTED
            REJECT -> PaymentStatus.REJECTED
        }
    }

    fun toPaymentAction(): PaymentAction {
        return when (this) {
            ACCEPT -> PaymentAction.ACCEPTED
            REJECT -> PaymentAction.REJECTED
        }
    }
}
