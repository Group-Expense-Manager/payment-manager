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
            it.amount shouldBe paymentEntity.amount
            it.fxData shouldBe paymentEntity.fxData
            it.createdAt shouldBe paymentEntity.createdAt
            it.updatedAt shouldBe paymentEntity.updatedAt
            it.attachmentId shouldBe paymentEntity.attachmentId
            it.status shouldBe paymentEntity.status
            it.history shouldBe paymentEntity.history
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
            it.amount shouldBe payment.amount
            it.fxData shouldBe payment.fxData
            it.createdAt shouldBe payment.createdAt
            it.updatedAt shouldBe payment.updatedAt
            it.attachmentId shouldBe payment.attachmentId
            it.status shouldBe payment.status
            it.history shouldBe payment.history
        }
    }
},)
