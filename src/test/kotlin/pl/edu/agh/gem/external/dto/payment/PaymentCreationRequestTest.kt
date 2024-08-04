package pl.edu.agh.gem.external.dto.payment

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.internal.model.payment.PaymentAction.CREATED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.util.createPaymentCreationRequest

class PaymentCreationRequestTest : ShouldSpec({

    should("Map to domain") {
        // given
        val paymentCreationRequest = createPaymentCreationRequest()

        // when
        val payment = paymentCreationRequest.toDomain(USER_ID, GROUP_ID)

        // then
        payment.shouldNotBeNull()
        payment.also {
            it.id.shouldNotBeNull()
            it.groupId shouldBe GROUP_ID
            it.creatorId shouldBe USER_ID
            it.recipientId shouldBe paymentCreationRequest.recipientId
            it.title shouldBe paymentCreationRequest.title
            it.type shouldBe paymentCreationRequest.type
            it.sum shouldBe paymentCreationRequest.sum
            it.baseCurrency shouldBe paymentCreationRequest.baseCurrency
            it.targetCurrency shouldBe paymentCreationRequest.targetCurrency
            it.exchangeRate.shouldBeNull()
            it.createdAt.shouldNotBeNull()
            it.updatedAt.shouldNotBeNull()
            it.attachmentId shouldBe paymentCreationRequest.attachmentId
            it.status shouldBe PENDING
            it.statusHistory shouldHaveSize 1
            it.statusHistory.first().also { entry ->
                entry.createdAt.shouldNotBeNull()
                entry.paymentAction shouldBe CREATED
                entry.participantId shouldBe USER_ID
                entry.comment shouldBe paymentCreationRequest.message
            }
        }
    }
},)
