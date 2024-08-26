package pl.edu.agh.gem.external.persistence

import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Query.query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.stereotype.Repository
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.filter.FilterOptions
import pl.edu.agh.gem.internal.model.payment.filter.SortOrder.ASCENDING
import pl.edu.agh.gem.internal.model.payment.filter.SortOrder.DESCENDING
import pl.edu.agh.gem.internal.model.payment.filter.SortedBy.DATE
import pl.edu.agh.gem.internal.model.payment.filter.SortedBy.TITLE
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

    override fun findByGroupId(groupId: String, filterOptions: FilterOptions?): List<Payment> {
        val query = Query().addCriteria(where(PaymentEntity::groupId).isEqualTo(groupId))

        filterOptions?.also {
            it.title?.also { title ->
                val regex = ".*$title.*"
                query.addCriteria(where(PaymentEntity::title).regex(regex, "i"))
            }

            it.status?.also { status ->
                query.addCriteria(where(PaymentEntity::status).isEqualTo(status))
            }

            it.creatorId?.also { creatorId ->
                query.addCriteria(where(PaymentEntity::creatorId).isEqualTo(creatorId))
            }

            val sortedByField = when (it.sortedBy) {
                TITLE -> PaymentEntity::title.name
                DATE -> PaymentEntity::date.name
            }

            val sort = when (it.sortOrder) {
                ASCENDING -> Sort.by(Sort.Order.asc(sortedByField))
                DESCENDING -> Sort.by(Sort.Order.desc(sortedByField))
            }
            query.with(sort)
        }

        return mongo.find(query, PaymentEntity::class.java).map(PaymentEntity::toDomain)
    }

    override fun delete(payment: Payment) {
        val query = query(where(PaymentEntity::id).isEqualTo(payment.id))
        mongo.remove(query, PaymentEntity::class.java)
    }
}
