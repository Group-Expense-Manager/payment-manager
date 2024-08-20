package pl.edu.agh.gem.internal.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.internal.model.payment.PaymentStatus
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.ACCEPTED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.REJECTED

class PaymentStatusTest : ShouldSpec({

    context("reduce correctly") {
        withData(
            Pair(listOf(REJECTED, ACCEPTED, PENDING), REJECTED),
            Pair(listOf(ACCEPTED, ACCEPTED, ACCEPTED), ACCEPTED),
            Pair(listOf(ACCEPTED, PENDING, ACCEPTED), PENDING),
        ) { (statuses, expectedStatus) ->
            // when
            val result = PaymentStatus.reduce(statuses)

            // then
            result shouldBe expectedStatus
        }
    }
},)
