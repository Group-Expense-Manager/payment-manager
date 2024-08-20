package pl.edu.agh.gem.external.dto.payment

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.util.createPayment

class PaymentUpdateResponseTest : ShouldSpec({

    should("map payment to paymentUpdateResponse correctly") {
        // given
        val payment = createPayment()

        // when
        val paymentUpdateResponse = payment.toPaymentUpdateResponse()

        // then
        paymentUpdateResponse.paymentId shouldBe payment.id
    }
},)
