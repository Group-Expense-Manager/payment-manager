package pl.edu.agh.gem.external.dto.payment

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.util.createPayment

class PaymentResponseTest : ShouldSpec({
    should("map Payment to PaymentResponse") {
        // given
        val payment = createPayment()

        // when
        val paymentResponse = payment.toPaymentResponse()

        // then
        paymentResponse.also {
            it.paymentId shouldBe payment.id
            it.creatorId shouldBe payment.creatorId
            it.recipientId shouldBe payment.recipientId
            it.title shouldBe payment.title
            it.type shouldBe payment.type.name
            it.amount shouldBe payment.amount.toAmountDto()
            it.fxData shouldBe payment.fxData
            it.date shouldBe payment.date
            it.createdAt.shouldNotBeNull()
            it.updatedAt.shouldNotBeNull()
            it.attachmentId shouldBe payment.attachmentId
            it.status shouldBe payment.status.name
            it.history shouldContainExactly payment.history.map { entity -> entity.toDto() }
        }
    }
},)
