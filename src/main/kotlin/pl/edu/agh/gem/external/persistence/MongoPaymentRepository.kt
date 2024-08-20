package pl.edu.agh.gem.external.persistence

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Repository
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.persistence.PaymentRepository

@Repository
class MongoPaymentRepository(
    private val mongo: MongoTemplate,

) : PaymentRepository {
    override fun save(payment: Payment): Payment {
        return mongo.save(payment.toEntity()).toDomain()
    }
}
