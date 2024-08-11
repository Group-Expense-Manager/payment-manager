package pl.edu.agh.gem.internal.persistence

import pl.edu.agh.gem.internal.model.payment.Payment

interface ArchivedPaymentRepository {
    fun add(payment: Payment)
}
