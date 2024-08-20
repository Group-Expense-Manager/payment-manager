package pl.edu.agh.gem.external.persistence

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
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

    override fun findByPaymentIdAndGroupId(paymentId: String, groupId: String): Payment? {
        val query = Query()
            .addCriteria(where(PaymentEntity::id).isEqualTo(paymentId))
            .addCriteria(where(PaymentEntity::groupId).isEqualTo(groupId))
        return mongo.findOne(query, PaymentEntity::class.java)?.toDomain()
    }

    override fun delete(payment: Payment) {
        val query = query(where(PaymentEntity::id).isEqualTo(payment.id))
        mongo.remove(query, PaymentEntity::class.java)
    }
}
