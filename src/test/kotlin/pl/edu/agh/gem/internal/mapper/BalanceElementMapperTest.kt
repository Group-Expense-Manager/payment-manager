package pl.edu.agh.gem.internal.mapper

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.helper.user.DummyUser.OTHER_USER_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.util.DummyData.ANOTHER_USER_ID
import pl.edu.agh.gem.util.DummyData.CURRENCY_1
import pl.edu.agh.gem.util.DummyData.CURRENCY_2
import pl.edu.agh.gem.util.Pair
import pl.edu.agh.gem.util.createAmount
import pl.edu.agh.gem.util.createBalanceElement
import pl.edu.agh.gem.util.createFxData
import pl.edu.agh.gem.util.createPayment

class BalanceElementMapperTest : ShouldSpec({

    val balanceElementMapper = BalanceElementMapper()

    context("map correctly") {
        withData(
            Pair(
                createPayment(
                    creatorId = USER_ID,
                    recipientId = OTHER_USER_ID,
                    amount = createAmount(
                        value = 50.toBigDecimal(),
                        currency = CURRENCY_1,
                    ),
                    fxData = createFxData(
                        targetCurrency = CURRENCY_2,
                        exchangeRate = "1.5".toBigDecimal(),
                    ),
                ),
                createBalanceElement(
                    value = 50.toBigDecimal(),
                    currency = CURRENCY_2,
                    exchangeRate = "1.5".toBigDecimal(),
                ),
            ),
            Pair(
                createPayment(
                    creatorId = OTHER_USER_ID,
                    recipientId = USER_ID,
                    amount = createAmount(
                        value = 50.toBigDecimal(),
                        currency = CURRENCY_1,
                    ),
                    fxData = null,
                ),
                createBalanceElement(
                    value = (-50).toBigDecimal(),
                    currency = CURRENCY_1,
                    exchangeRate = null,
                ),
            ),
            Pair(
                createPayment(
                    creatorId = OTHER_USER_ID,
                    recipientId = ANOTHER_USER_ID,
                ),
                null,
            ),
        ) { (payment, expectedBalanceElement) ->
            // when
            val actualBalanceElement = balanceElementMapper.mapToBalanceElement(USER_ID, payment)

            // then
            actualBalanceElement shouldBe expectedBalanceElement
        }
    }
},)
