package pl.edu.agh.gem.external.dto.payment

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.util.createBalanceElement

class UserBalanceResponseTest : ShouldSpec({
    should("map to UserBalanceResponse") {
        // given
        val balanceElement = createBalanceElement()

        // when
        val userBalanceResponse = listOf(balanceElement).toUserBalanceResponse(USER_ID)

        // then
        userBalanceResponse.also {
            it.userId shouldBe USER_ID
            it.elements shouldHaveSize 1
            it.elements.first().also { element ->
                element.value shouldBe balanceElement.value
                element.currency shouldBe balanceElement.currency
                element.exchangeRate shouldBe balanceElement.exchangeRate
            }
        }
    }
},)
