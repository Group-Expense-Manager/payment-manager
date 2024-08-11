package pl.edu.agh.gem.external.persistence

import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Repository
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.persistence.ArchivedPaymentRepository

@Repository
class MongoArchivedPaymentRepository(
    private val mongo: MongoTemplate,
) : ArchivedPaymentRepository {
    override fun add(payment: Payment) {
        mongo.insert(payment.toEntity())
    }

    private fun Payment.toEntity() =
        ArchivedPaymentEntity(
            id = id,
            groupId = groupId,
            creatorId = creatorId,
            recipientId = recipientId,
            title = title,
            type = type,
            amount = amount,
            fxData = fxData,
            date = date,
            createdAt = createdAt,
            updatedAt = updatedAt,
            attachmentId = attachmentId,
            status = status,
            history = history,
        )
}
