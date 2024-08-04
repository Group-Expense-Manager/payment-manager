package pl.edu.agh.gem.internal.persistence

import pl.edu.agh.gem.internal.model.payment.Payment

interface PaymentRepository {
    fun save(payment: Payment): Payment
}
