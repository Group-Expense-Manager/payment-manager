package pl.edu.agh.gem.integration.persistence

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.group.DummyGroup.OTHER_GROUP_ID
import pl.edu.agh.gem.integration.BaseIntegrationSpec
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.ACCEPTED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.REJECTED
import pl.edu.agh.gem.internal.model.payment.filter.SortOrder.ASCENDING
import pl.edu.agh.gem.internal.model.payment.filter.SortOrder.DESCENDING
import pl.edu.agh.gem.internal.model.payment.filter.SortedBy.DATE
import pl.edu.agh.gem.internal.model.payment.filter.SortedBy.TITLE
import pl.edu.agh.gem.internal.persistence.PaymentRepository
import pl.edu.agh.gem.util.createFilterOptions
import pl.edu.agh.gem.util.createPayment
import java.time.Instant.ofEpochMilli

class MongoPaymentRepositoryIT(
    private val paymentRepository: PaymentRepository,
) : BaseIntegrationSpec({
    should("delete payment") {
        // given
        val payment = createPayment()
        paymentRepository.save(payment)

        // when
        paymentRepository.delete(payment)

        // then
        paymentRepository.findByPaymentIdAndGroupId(payment.id, payment.groupId).also {
            it.shouldBeNull()
        }
    }

    should("return empty list when there is no payment with given groupId") {
        // given
        val payment = createPayment(groupId = OTHER_GROUP_ID)
        paymentRepository.save(payment)

        // when
        val payments = paymentRepository.findByGroupId(GROUP_ID)

        // then
        payments.shouldBeEmpty()
    }

    should("find payment with given groupId") {
        // given
        val payment = createPayment(groupId = GROUP_ID)
        paymentRepository.save(payment)

        // when
        val payments = paymentRepository.findByGroupId(GROUP_ID)

        // then
        payments.also {
            it.size shouldBe 1
            it.first().id shouldBe payment.id
        }
    }

    should("find payment with given groupId and containing title") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, title = "Pizza in Krakow")
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, title = "The best burger")
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, title = "Spaghetti with Andrzej")

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        val filterOptions = createFilterOptions(title = "KRA")

        // when
        val payments = paymentRepository.findByGroupId(GROUP_ID, filterOptions)

        // then
        payments.also {
            it.size shouldBe 1
            it.first().id shouldBe payment1.id
        }
    }

    should("find payment with given groupId and status") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, status = REJECTED)
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, status = ACCEPTED)
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, status = PENDING)

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        val filterOptions = createFilterOptions(status = PENDING)

        // when
        val payments = paymentRepository.findByGroupId(GROUP_ID, filterOptions)

        // then
        payments.also {
            it.size shouldBe 1
            it.first().id shouldBe payment3.id
        }
    }

    should("find payment with given groupId and creatorId") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, creatorId = "1")
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, creatorId = "2")
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, creatorId = "1")

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        val filterOptions = createFilterOptions(creatorId = "1")

        // when
        val payments = paymentRepository.findByGroupId(GROUP_ID, filterOptions)

        // then
        payments.map { it.id } shouldContainExactly listOf(payment1.id, payment3.id)
    }

    should("find payment with given groupId and sorted by title ascending") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, title = "Pizza in Krakow")
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, title = "The best burger")
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, title = "Spaghetti with Andrzej")

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        val filterOptions = createFilterOptions(sortedBy = TITLE, sortOrder = ASCENDING)

        // when
        val payments = paymentRepository.findByGroupId(GROUP_ID, filterOptions)

        // then
        payments.also {
            payments.map { it.id } shouldContainExactly listOf(payment1.id, payment3.id, payment2.id)
        }
    }

    should("find payment with given groupId and sorted by title descending") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, title = "Pizza in Krakow")
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, title = "The best burger")
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, title = "Spaghetti with Andrzej")

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        val filterOptions = createFilterOptions(sortedBy = TITLE, sortOrder = DESCENDING)

        // when
        val payments = paymentRepository.findByGroupId(GROUP_ID, filterOptions)

        // then
        payments.also {
            payments.map { it.id } shouldContainExactly listOf(payment2.id, payment3.id, payment1.id)
        }
    }

    should("find payment with given groupId and sorted by payment date ascending") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, date = ofEpochMilli(2))
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, date = ofEpochMilli(3))
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, date = ofEpochMilli(1))

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        val filterOptions = createFilterOptions(sortedBy = DATE, sortOrder = ASCENDING)

        // when
        val payments = paymentRepository.findByGroupId(GROUP_ID, filterOptions)

        // then
        payments.also {
            payments.map { it.id } shouldContainExactly listOf(payment3.id, payment1.id, payment2.id)
        }
    }

    should("find payment with given groupId and sorted by payment date descending") {
        // given
        val payment1 = createPayment(id = "1", groupId = GROUP_ID, date = ofEpochMilli(2))
        val payment2 = createPayment(id = "2", groupId = GROUP_ID, date = ofEpochMilli(3))
        val payment3 = createPayment(id = "3", groupId = GROUP_ID, date = ofEpochMilli(1))

        listOf(payment1, payment2, payment3).forEach { paymentRepository.save(it) }

        val filterOptions = createFilterOptions(sortedBy = DATE, sortOrder = DESCENDING)

        // when
        val payments = paymentRepository.findByGroupId(GROUP_ID, filterOptions)

        // then
        payments.also {
            payments.map { it.id } shouldContainExactly listOf(payment2.id, payment1.id, payment3.id)
        }
    }
},)
