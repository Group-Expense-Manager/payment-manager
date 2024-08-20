package pl.edu.agh.gem.external.dto.payment

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.util.DummyData.PAYMENT_ID
import pl.edu.agh.gem.util.createPaymentUpdateRequest

class PaymentUpdateRequestTest : ShouldSpec({

    should("Map to domain correctly") {
        // given
        val paymentUpdateRequest = createPaymentUpdateRequest()

        // when
        val paymentUpdate = paymentUpdateRequest.toDomain(PAYMENT_ID, GROUP_ID, USER_ID)

        // then
        paymentUpdate.also {
            it.id shouldBe PAYMENT_ID
            it.groupId shouldBe GROUP_ID
            it.userId shouldBe USER_ID
            it.title shouldBe paymentUpdateRequest.title
            it.type shouldBe paymentUpdateRequest.type
            it.amount shouldBe paymentUpdateRequest.amount.toDomain()
            it.targetCurrency shouldBe paymentUpdateRequest.targetCurrency
            it.date shouldBe paymentUpdateRequest.date
            it.message shouldBe paymentUpdateRequest.message
        }
    }
},)
