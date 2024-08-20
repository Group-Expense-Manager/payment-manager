package pl.edu.agh.gem.internal.model.payment

enum class PaymentStatus {
    ACCEPTED,
    REJECTED,
    PENDING,
    ;

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
