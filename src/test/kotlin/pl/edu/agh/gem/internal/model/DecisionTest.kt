package pl.edu.agh.gem.internal.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.internal.model.payment.Decision.ACCEPT
import pl.edu.agh.gem.internal.model.payment.Decision.REJECT
import pl.edu.agh.gem.internal.model.payment.PaymentAction
import pl.edu.agh.gem.internal.model.payment.PaymentStatus

class DecisionTest : ShouldSpec({

    context("map to payment status correctly") {
        withData(
            Pair(ACCEPT, PaymentStatus.ACCEPTED),
            Pair(REJECT, PaymentStatus.REJECTED),
        ) { (decision, expectedStatus) ->
            // when
            val result = decision.toPaymentStatus()

            // then
            result shouldBe expectedStatus
        }
    }

    context("map to payment action correctly") {
        withData(
            Pair(ACCEPT, PaymentAction.ACCEPTED),
            Pair(REJECT, PaymentAction.REJECTED),
        ) { (decision, expectedAction) ->
            // when
            val result = decision.toPaymentAction()

            // then
            result shouldBe expectedAction
        }
    }
},)
