package pl.edu.agh.gem.external.dto.payment

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.internal.model.payment.Amount
import pl.edu.agh.gem.internal.model.payment.FxData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.util.createPayment
import java.math.BigDecimal
import java.time.Instant

class GroupPaymentsResponseTest : ShouldSpec({

    should("map Payments to AcceptedGroupPaymentsResponse") {
        // given
        val payment = createPayment()
        // when
        val groupPaymentsResponse = listOf(payment).toAcceptedGroupPaymentsResponse(GROUP_ID)

        // then
        groupPaymentsResponse.groupId shouldBe GROUP_ID
        groupPaymentsResponse.payments shouldHaveSize 1
        groupPaymentsResponse.payments.first().also {
            it.creatorId shouldBe payment.creatorId
            it.recipientId shouldBe payment.recipientId
            it.title shouldBe payment.title
            it.amount shouldBe payment.amount.toAmountDto()
            it.fxData shouldBe payment.fxData?.toDto()
            it.date shouldBe payment.date
        }
    }

    should("map multiple Payments to AcceptedGroupPaymentsResponse") {
        // given
        val creatorIds = listOf("creatorId1", "creatorId2", "creatorId3")
        val recipientIds = listOf("recipientId1", "recipientId2", "recipientId3")
        val titles = listOf("title1", "title2", "title3")
        val amounts = listOf(
            Amount(value = BigDecimal.ONE, currency = "PLN"),
            Amount(value = BigDecimal.TWO, currency = "EUR"),
            Amount(value = BigDecimal.TEN, currency = "USD"),
        )

        val fxDataList = listOf(
            null,
            FxData(targetCurrency = "PLN", exchangeRate = BigDecimal.TWO),
            FxData(targetCurrency = "EUR", exchangeRate = BigDecimal.ONE),
        )

        val dates = listOf(
            Instant.ofEpochSecond(1000),
            Instant.ofEpochSecond(2000),
            Instant.ofEpochSecond(3000),
        )
        val payments = creatorIds.mapIndexed { index, creatorId ->
            createPayment(
                creatorId = creatorId,
                recipientId = recipientIds[index],
                title = titles[index],
                amount = amounts[index],
                fxData = fxDataList[index],
                date = dates[index],
            )
        }

        // when
        val groupPaymentsResponse = payments.toAcceptedGroupPaymentsResponse(GROUP_ID)

        // then
        groupPaymentsResponse.groupId shouldBe GROUP_ID
        groupPaymentsResponse.payments.also {
            it shouldHaveSize 3
            it.map { groupPaymentsDto -> groupPaymentsDto.creatorId } shouldContainExactly creatorIds
            it.map { groupPaymentsDto -> groupPaymentsDto.recipientId } shouldContainExactly recipientIds
            it.map { groupPaymentsDto -> groupPaymentsDto.title } shouldContainExactly titles
            it.map { groupPaymentsDto -> groupPaymentsDto.amount } shouldContainExactly amounts.map { amount -> amount.toAmountDto() }
            it.map { groupPaymentsDto -> groupPaymentsDto.fxData } shouldContainExactly fxDataList.map { amount -> amount?.toDto() }
            it.map { groupPaymentsDto -> groupPaymentsDto.date } shouldContainExactly dates
        }
    }

    should("return empty list when there are no payments") {
        // given
        val payments = listOf<Payment>()

        // when
        val groupPaymentsResponse = payments.toAcceptedGroupPaymentsResponse(GROUP_ID)

        // then
        groupPaymentsResponse.also {
            it.groupId shouldBe GROUP_ID
            it.payments shouldBe listOf()
        }
    }
},)
