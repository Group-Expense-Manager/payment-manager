package pl.edu.agh.gem.internal.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.internal.model.payment.PaymentAction.CREATED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.util.DummyData.ATTACHMENT_ID
import pl.edu.agh.gem.util.createFxData
import pl.edu.agh.gem.util.createPaymentCreation

class PaymentCreationTest : ShouldSpec({

    should("Map to domain") {
        // given
        val paymentCreation = createPaymentCreation()

        // when
        val payment = paymentCreation.toPayment(createFxData(), ATTACHMENT_ID)

        // then
        payment.shouldNotBeNull()
        payment.also {
            it.id.shouldNotBeNull()
            it.groupId shouldBe GROUP_ID
            it.creatorId shouldBe USER_ID
            it.recipientId shouldBe paymentCreation.recipientId
            it.title shouldBe paymentCreation.title
            it.type shouldBe paymentCreation.type
            it.amount shouldBe paymentCreation.amount
            it.fxData?.targetCurrency shouldBe paymentCreation.targetCurrency
            it.createdAt.shouldNotBeNull()
            it.updatedAt.shouldNotBeNull()
            it.attachmentId shouldBe paymentCreation.attachmentId
            it.status shouldBe PENDING
            it.history shouldHaveSize 1
            it.history.first().also { entry ->
                entry.createdAt.shouldNotBeNull()
                entry.paymentAction shouldBe CREATED
                entry.participantId shouldBe USER_ID
                entry.comment shouldBe paymentCreation.message
            }
        }
    }
},)
