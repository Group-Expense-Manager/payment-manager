package pl.edu.agh.gem.external.dto.payment

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.util.createPaymentCreationRequest

class PaymentCreationRequestTest : ShouldSpec({
    should("Map to domain") {
        // given
        val paymentCreationRequest = createPaymentCreationRequest()

        // when
        val paymentCreation = paymentCreationRequest.toDomain(USER_ID, GROUP_ID)

        // then
        paymentCreation.shouldNotBeNull()
        paymentCreation.also {
            it.groupId shouldBe GROUP_ID
            it.creatorId shouldBe USER_ID
            it.recipientId shouldBe paymentCreationRequest.recipientId
            it.title shouldBe paymentCreationRequest.title
            it.type shouldBe paymentCreationRequest.type
            it.amount.also { amount ->
                amount.value shouldBe paymentCreationRequest.amount.value
                amount.currency shouldBe paymentCreationRequest.amount.currency
            }
            it.targetCurrency shouldBe paymentCreationRequest.targetCurrency
            it.attachmentId shouldBe paymentCreationRequest.attachmentId
        }
    }
},)
