package pl.edu.agh.gem.integration.persistence

import io.kotest.matchers.nulls.shouldBeNull
import pl.edu.agh.gem.integration.BaseIntegrationSpec
import pl.edu.agh.gem.internal.persistence.PaymentRepository
import pl.edu.agh.gem.util.createPayment

class MongoPaymentRepositoryIT(
    private val paymentRepository: PaymentRepository,
) : BaseIntegrationSpec({
    should("delete payment") {
        // given
        val payment = createPayment()
        paymentRepository.save(payment)

        // when
        paymentRepository.delete(payment)

        // then
        paymentRepository.findByPaymentIdAndGroupId(payment.id, payment.groupId).also {
            it.shouldBeNull()
        }
    }
},)
