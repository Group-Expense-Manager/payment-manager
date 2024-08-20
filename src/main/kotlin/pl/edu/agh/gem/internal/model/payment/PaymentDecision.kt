package pl.edu.agh.gem.internal.model.payment

data class PaymentDecision(
    val userId: String,
    val paymentId: String,
    val groupId: String,
    val decision: Decision,
    val message: String?,
)
