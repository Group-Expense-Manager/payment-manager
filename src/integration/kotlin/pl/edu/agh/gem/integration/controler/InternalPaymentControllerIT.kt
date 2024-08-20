package pl.edu.agh.gem.integration.controler

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpStatus.OK
import pl.edu.agh.gem.assertion.shouldBody
import pl.edu.agh.gem.assertion.shouldHaveHttpStatus
import pl.edu.agh.gem.external.dto.payment.GroupActivitiesResponse
import pl.edu.agh.gem.external.dto.payment.toAmountDto
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.group.DummyGroup.OTHER_GROUP_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.helper.user.createGemUser
import pl.edu.agh.gem.integration.BaseIntegrationSpec
import pl.edu.agh.gem.integration.ability.ServiceTestClient
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.ACCEPTED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.REJECTED
import pl.edu.agh.gem.internal.model.payment.filter.SortOrder.ASCENDING
import pl.edu.agh.gem.internal.model.payment.filter.SortOrder.DESCENDING
import pl.edu.agh.gem.internal.model.payment.filter.SortedBy.DATE
import pl.edu.agh.gem.internal.model.payment.filter.SortedBy.TITLE
import pl.edu.agh.gem.internal.persistence.PaymentRepository
import pl.edu.agh.gem.util.createPayment
import java.time.Instant.ofEpochMilli

class InternalPaymentControllerIT(
    private val service: ServiceTestClient,
    private val paymentRepository: PaymentRepository,
) : BaseIntegrationSpec({
    should("get group activities") {
        // given
        val payment = createPayment(groupId = GROUP_ID)
        paymentRepository.save(payment)

        // when
        val response = service.getGroupActivitiesResponse(createGemUser(USER_ID), GROUP_ID)

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<GroupActivitiesResponse> {
            groupId shouldBe GROUP_ID
            payments shouldHaveSize 1
            payments.first().also {
                it.paymentId shouldBe payment.id
                it.creatorId shouldBe payment.creatorId
                it.recipientId shouldBe payment.recipientId
                it.title shouldBe payment.title
                it.amount shouldBe payment.amount.toAmountDto()
                it.targetCurrency shouldBe payment.fxData?.targetCurrency
                it.status shouldBe payment.status
                it.date shouldBe payment.date
            }
        }
    }

    should("get empty list when attempting to get group activities") {
        // given
        val payment = createPayment(groupId = OTHER_GROUP_ID)
        paymentRepository.save(payment)

        // when
        val response = service.getGroupActivitiesResponse(createGemUser(USER_ID), GROUP_ID)

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<GroupActivitiesResponse> {
            groupId shouldBe GROUP_ID
            payments shouldHaveSize 0
        }
    }

    should("get group activities with given title") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, title = "Pizza in Krakow")
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, title = "The best burger")
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, title = "Spaghetti with Andrzej")

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        // when
        val response = service.getGroupActivitiesResponse(createGemUser(USER_ID), GROUP_ID, title = "KRA")

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<GroupActivitiesResponse> {
            groupId shouldBe GROUP_ID
            payments shouldHaveSize 1
            payments.first().paymentId shouldBe payment1.id
        }
    }

    should("get group activities with given status") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, status = REJECTED)
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, status = ACCEPTED)
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, status = PENDING)

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        // when
        val response = service.getGroupActivitiesResponse(createGemUser(USER_ID), GROUP_ID, status = PENDING)

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<GroupActivitiesResponse> {
            groupId shouldBe GROUP_ID
            payments shouldHaveSize 1
            payments.first().paymentId shouldBe payment3.id
        }
    }

    should("get group activities with given creatorId") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, creatorId = "1")
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, creatorId = "2")
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, creatorId = "1")

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        // when
        val response = service.getGroupActivitiesResponse(createGemUser(USER_ID), GROUP_ID, creatorId = "1")

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<GroupActivitiesResponse> {
            groupId shouldBe GROUP_ID
            payments shouldHaveSize 2
            payments.map { it.paymentId } shouldContainExactly listOf(payment1.id, payment3.id)
        }
    }

    should("get group activities sorted by title") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, title = "Pizza in Krakow")
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, title = "The best burger")
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, title = "Spaghetti with Andrzej")

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        // when
        val response = service.getGroupActivitiesResponse(createGemUser(USER_ID), GROUP_ID, sortedBy = TITLE)

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<GroupActivitiesResponse> {
            groupId shouldBe GROUP_ID
            payments shouldHaveSize 3
            payments.map { it.paymentId } shouldContainExactly listOf(payment1.id, payment3.id, payment2.id)
        }
    }

    should("get group activities sorted by date") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, date = ofEpochMilli(2))
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, date = ofEpochMilli(3))
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, date = ofEpochMilli(1))

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        // when
        val response = service.getGroupActivitiesResponse(createGemUser(USER_ID), GROUP_ID, sortedBy = DATE)

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<GroupActivitiesResponse> {
            groupId shouldBe GROUP_ID
            payments shouldHaveSize 3
            payments.map { it.paymentId } shouldContainExactly listOf(payment3.id, payment1.id, payment2.id)
        }
    }

    should("get group activities sorted by date ascending") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, date = ofEpochMilli(2))
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, date = ofEpochMilli(3))
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, date = ofEpochMilli(1))

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        // when
        val response = service.getGroupActivitiesResponse(createGemUser(USER_ID), GROUP_ID, sortOrder = ASCENDING)

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<GroupActivitiesResponse> {
            groupId shouldBe GROUP_ID
            payments shouldHaveSize 3
            payments.map { it.paymentId } shouldContainExactly listOf(payment3.id, payment1.id, payment2.id)
        }
    }

    should("get group activities sorted by date descending") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, date = ofEpochMilli(2))
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, date = ofEpochMilli(3))
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, date = ofEpochMilli(1))

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        // when
        val response = service.getGroupActivitiesResponse(createGemUser(USER_ID), GROUP_ID, sortOrder = DESCENDING)

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<GroupActivitiesResponse> {
            groupId shouldBe GROUP_ID
            payments shouldHaveSize 3
            payments.map { it.paymentId } shouldContainExactly listOf(payment2.id, payment1.id, payment3.id)
        }
    }
},)
