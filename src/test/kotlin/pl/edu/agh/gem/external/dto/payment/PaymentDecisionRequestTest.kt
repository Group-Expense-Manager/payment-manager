package pl.edu.agh.gem.external.dto.payment

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.util.createPaymentDecisionRequest

class PaymentDecisionRequestTest : ShouldSpec({
    should("Map correctly to Decision") {
        // given
        val paymentDecision = createPaymentDecisionRequest()

        // when
        val result = paymentDecision.toDomain(USER_ID)

        // then
        result shouldNotBe null
        result.also {
            it.paymentId shouldBe paymentDecision.paymentId
            it.groupId shouldBe paymentDecision.groupId
            it.decision shouldBe paymentDecision.decision
            it.message shouldBe paymentDecision.message
        }
    }
},)
