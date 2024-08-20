package pl.edu.agh.gem.internal.model.payment

import java.time.Instant
import java.time.Instant.now

data class PaymentHistoryEntry(
    val participantId: String,
    val paymentAction: PaymentAction,
    val createdAt: Instant = now(),
    val comment: String? = null,
)
