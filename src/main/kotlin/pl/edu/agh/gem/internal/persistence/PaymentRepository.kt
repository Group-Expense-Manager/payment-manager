package pl.edu.agh.gem.internal.persistence

import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.filter.FilterOptions

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findByPaymentIdAndGroupId(paymentId: String, groupId: String): Payment?
    fun findByGroupId(groupId: String, filterOptions: FilterOptions? = null): List<Payment>
    fun delete(payment: Payment)
}
