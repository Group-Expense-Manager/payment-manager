package pl.edu.agh.gem.external.dto.payment

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.internal.model.payment.Amount
import pl.edu.agh.gem.internal.model.payment.FxData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.ACCEPTED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.REJECTED
import pl.edu.agh.gem.util.createPayment
import java.math.BigDecimal
import java.time.Instant

class GroupActivitiesResponseTest : ShouldSpec({

    should("map Payment to GroupActivitiesResponse") {
        // given
        val payment = createPayment()

        // when
        val groupActivitiesResponse = listOf(payment).toGroupActivitiesResponse(GROUP_ID)

        // then
        groupActivitiesResponse.groupId shouldBe GROUP_ID
        groupActivitiesResponse.payments shouldHaveSize 1
        groupActivitiesResponse.payments.first().also {
            it.paymentId shouldBe payment.id
            it.creatorId shouldBe payment.creatorId
            it.recipientId shouldBe payment.recipientId
            it.title shouldBe payment.title
            it.amount shouldBe payment.amount.toAmountDto()
            it.fxData shouldBe payment.fxData?.toDto()
            it.status shouldBe payment.status
            it.date shouldBe payment.date
        }
    }

    should("map multiple Payments to GroupActivitiesResponse") {
        // given
        val paymentIds = listOf("paymentId1", "paymentId2", "paymentId3")
        val creatorIds = listOf("creatorId1", "creatorId2", "creatorId3")
        val recipientIds = listOf("recipientId1", "recipientId2", "recipientId3")

        val titles = listOf("title1", "title2", "title3")
        val amounts = listOf(
            Amount(value = BigDecimal.ONE, currency = "PLN"),
            Amount(value = BigDecimal.TWO, currency = "EUR"),
            Amount(value = BigDecimal.TEN, currency = "USD"),
        )
        val fxData = listOf(
            FxData(targetCurrency = "EUR", exchangeRate = "2".toBigDecimal()),
            null,
            FxData(targetCurrency = "PLN", exchangeRate = "3".toBigDecimal()),

        )

        val statuses = listOf(PENDING, ACCEPTED, REJECTED)
        val dates = listOf(
            Instant.ofEpochSecond(1000),
            Instant.ofEpochSecond(2000),
            Instant.ofEpochSecond(3000),
        )
        val payments = paymentIds.mapIndexed { index, paymentId ->
            createPayment(
                id = paymentId,
                creatorId = creatorIds[index],
                recipientId = recipientIds[index],
                title = titles[index],
                amount = amounts[index],
                fxData = fxData[index],
                status = statuses[index],
                date = dates[index],
            )
        }

        // when
        val groupActivitiesResponse = payments.toGroupActivitiesResponse(GROUP_ID)

        // then
        groupActivitiesResponse.groupId shouldBe GROUP_ID
        groupActivitiesResponse.payments.also {
            it shouldHaveSize 3
            it.map { groupPaymentsDto -> groupPaymentsDto.paymentId } shouldContainExactly paymentIds
            it.map { groupPaymentsDto -> groupPaymentsDto.creatorId } shouldContainExactly creatorIds
            it.map { groupPaymentsDto -> groupPaymentsDto.recipientId } shouldContainExactly recipientIds
            it.map { groupPaymentsDto -> groupPaymentsDto.title } shouldContainExactly titles
            it.map { groupPaymentsDto -> groupPaymentsDto.amount } shouldContainExactly amounts.map { amount -> amount.toAmountDto() }
            it.map { groupPaymentsDto -> groupPaymentsDto.fxData } shouldContainExactly fxData.map { fxData -> fxData?.toDto() }
            it.map { groupPaymentsDto -> groupPaymentsDto.status } shouldContainExactly statuses
            it.map { groupPaymentsDto -> groupPaymentsDto.date } shouldContainExactly dates
        }
    }

    should("return empty list when there are no payments") {
        // given
        val payments = listOf<Payment>()

        // when
        val groupActivitiesResponse = payments.toGroupActivitiesResponse(GROUP_ID)

        // then
        groupActivitiesResponse.also {
            it.groupId shouldBe GROUP_ID
            it.payments shouldBe listOf()
        }
    }
},)
