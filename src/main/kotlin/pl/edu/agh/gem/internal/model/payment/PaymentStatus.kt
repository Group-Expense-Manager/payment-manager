package pl.edu.agh.gem.internal.model.payment

enum class PaymentStatus {
    ACCEPTED,
    REJECTED,
    PENDING,
    ;

    fun changedToAccepted(currentStatus: PaymentStatus): Boolean {
        return this != ACCEPTED && currentStatus == ACCEPTED
    }

    fun changedFromAccepted(previousStatus: PaymentStatus): Boolean {
        return previousStatus == ACCEPTED && this != ACCEPTED
    }

    companion object {
        fun reduce(statuses: List<PaymentStatus>): PaymentStatus {
            return when {
                statuses.contains(REJECTED) -> REJECTED
                statuses.all { it == ACCEPTED } -> ACCEPTED
                else -> PENDING
            }
        }
    }
}
