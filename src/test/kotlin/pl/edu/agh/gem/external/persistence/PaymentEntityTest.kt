package pl.edu.agh.gem.external.persistence

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.util.createPayment
import pl.edu.agh.gem.util.createPaymentEntity

class PaymentEntityTest : ShouldSpec({
    should("map entity to domain correctly") {
        // given
        val paymentEntity = createPaymentEntity()

        // when
        val payment = paymentEntity.toDomain()

        // then
        payment.shouldNotBeNull()
        payment.also {
            it.id shouldBe paymentEntity.id
            it.groupId shouldBe paymentEntity.groupId
            it.creatorId shouldBe paymentEntity.creatorId
            it.recipientId shouldBe paymentEntity.recipientId
            it.title shouldBe paymentEntity.title
            it.type shouldBe paymentEntity.type
            it.sum shouldBe paymentEntity.sum
            it.baseCurrency shouldBe paymentEntity.baseCurrency
            it.targetCurrency shouldBe paymentEntity.targetCurrency
            it.exchangeRate shouldBe paymentEntity.exchangeRate
            it.createdAt shouldBe paymentEntity.createdAt
            it.updatedAt shouldBe paymentEntity.updatedAt
            it.attachmentId shouldBe paymentEntity.attachmentId
            it.status shouldBe paymentEntity.status
            it.statusHistory shouldBe paymentEntity.statusHistory
        }
    }

    should("map payment to entity correctly") {
        // given
        val payment = createPayment()

        // when
        val paymentEntity = payment.toEntity()

        // then
        paymentEntity.shouldNotBeNull()
        paymentEntity.also {
            it.id shouldBe payment.id
            it.groupId shouldBe payment.groupId
            it.creatorId shouldBe payment.creatorId
            it.recipientId shouldBe payment.recipientId
            it.title shouldBe payment.title
            it.type shouldBe payment.type
            it.sum shouldBe payment.sum
            it.baseCurrency shouldBe payment.baseCurrency
            it.targetCurrency shouldBe payment.targetCurrency
            it.exchangeRate shouldBe payment.exchangeRate
            it.createdAt shouldBe payment.createdAt
            it.updatedAt shouldBe payment.updatedAt
            it.attachmentId shouldBe payment.attachmentId
            it.status shouldBe payment.status
            it.statusHistory shouldBe payment.statusHistory
        }
    }
},)
