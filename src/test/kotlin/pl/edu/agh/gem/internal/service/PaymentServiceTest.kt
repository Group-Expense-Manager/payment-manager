package pl.edu.agh.gem.internal.service

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.group.createGroupMembers
import pl.edu.agh.gem.helper.user.DummyUser.OTHER_USER_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.internal.client.AttachmentStoreClient
import pl.edu.agh.gem.internal.client.CurrencyManagerClient
import pl.edu.agh.gem.internal.client.GroupManagerClient
import pl.edu.agh.gem.internal.model.attachment.GroupAttachment
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentAction.CREATED
import pl.edu.agh.gem.internal.model.payment.PaymentAction.EDITED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.ACCEPTED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.persistence.ArchivedPaymentRepository
import pl.edu.agh.gem.internal.persistence.PaymentRepository
import pl.edu.agh.gem.util.DummyData.ANOTHER_USER_ID
import pl.edu.agh.gem.util.DummyData.ATTACHMENT_ID
import pl.edu.agh.gem.util.DummyData.CURRENCY_1
import pl.edu.agh.gem.util.DummyData.CURRENCY_2
import pl.edu.agh.gem.util.DummyData.EXCHANGE_RATE_VALUE
import pl.edu.agh.gem.util.DummyData.PAYMENT_ID
import pl.edu.agh.gem.util.createAmount
import pl.edu.agh.gem.util.createCurrencies
import pl.edu.agh.gem.util.createExchangeRate
import pl.edu.agh.gem.util.createFilterOptions
import pl.edu.agh.gem.util.createFxData
import pl.edu.agh.gem.util.createGroup
import pl.edu.agh.gem.util.createPayment
import pl.edu.agh.gem.util.createPaymentCreation
import pl.edu.agh.gem.util.createPaymentDecision
import pl.edu.agh.gem.util.createPaymentUpdate
import pl.edu.agh.gem.util.createPaymentUpdateFromPayment
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_AVAILABLE
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES
import pl.edu.agh.gem.validation.ValidationMessage.NO_MODIFICATION
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_IS_CREATOR
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_NOT_GROUP_MEMBER
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES
import pl.edu.agh.gem.validation.ValidationMessage.USER_NOT_CREATOR
import pl.edu.agh.gem.validation.ValidationMessage.USER_NOT_RECIPIENT
import pl.edu.agh.gem.validator.ValidatorsException
import java.math.BigDecimal
import java.time.Instant

class PaymentServiceTest : ShouldSpec({
    val groupManagerClient = mock<GroupManagerClient> { }
    val currencyManagerClient = mock<CurrencyManagerClient> {}
    val attachmentStoreClient = mock<AttachmentStoreClient> {}
    val paymentRepository = mock<PaymentRepository> {}
    val archivedPaymentRepository = mock<ArchivedPaymentRepository> {}

    val paymentService = PaymentService(
        groupManagerClient,
        currencyManagerClient,
        attachmentStoreClient,
        paymentRepository,
        archivedPaymentRepository,
    )

    should("create payment when attachmentId is provided") {
        // given
        val paymentCreation = createPaymentCreation()
        val group = createGroup(currencies = createCurrencies(CURRENCY_1, CURRENCY_2))
        val exchangeRate = createExchangeRate()
        whenever(currencyManagerClient.getAvailableCurrencies()).thenReturn(createCurrencies(CURRENCY_1, CURRENCY_2))
        whenever(currencyManagerClient.getExchangeRate(eq(CURRENCY_1), eq(CURRENCY_2), any())).thenReturn(exchangeRate)
        whenever(paymentRepository.save(anyVararg(Payment::class))).thenAnswer { it.arguments[0] }

        // when
        val result = paymentService.createPayment(group, paymentCreation)

        // then
        result.also {
            it.id.shouldNotBeNull()
            it.groupId shouldBe GROUP_ID
            it.creatorId shouldBe USER_ID
            it.recipientId shouldBe paymentCreation.recipientId
            it.title shouldBe paymentCreation.title
            it.type shouldBe paymentCreation.type
            it.amount shouldBe paymentCreation.amount
            it.fxData.also { fxData ->
                fxData?.targetCurrency shouldBe paymentCreation.targetCurrency
                fxData?.exchangeRate shouldBe exchangeRate.value
            }
            it.createdAt.shouldNotBeNull()
            it.updatedAt.shouldNotBeNull()
            it.attachmentId shouldBe paymentCreation.attachmentId
            it.status shouldBe PENDING
            it.history shouldHaveSize 1
            it.history.first().also { entry ->
                entry.createdAt.shouldNotBeNull()
                entry.paymentAction shouldBe CREATED
                entry.participantId shouldBe USER_ID
                entry.comment shouldBe paymentCreation.message
            }
        }

        verify(currencyManagerClient, times(1)).getAvailableCurrencies()
        verify(currencyManagerClient, times(1)).getExchangeRate(eq(CURRENCY_1), eq(CURRENCY_2), any())
        verify(paymentRepository, times(1)).save(anyVararg(Payment::class))
    }

    should("create payment when attachmentId is not provided") {
        // given
        val paymentCreation = createPaymentCreation(attachmentId = null)
        val group = createGroup(currencies = createCurrencies(CURRENCY_1, CURRENCY_2))
        val exchangeRate = createExchangeRate()
        whenever(currencyManagerClient.getAvailableCurrencies()).thenReturn(createCurrencies(CURRENCY_1, CURRENCY_2))
        whenever(currencyManagerClient.getExchangeRate(eq(CURRENCY_1), eq(CURRENCY_2), any())).thenReturn(exchangeRate)
        whenever(attachmentStoreClient.generateBlankAttachment(GROUP_ID, USER_ID)).thenReturn(GroupAttachment(ATTACHMENT_ID))
        whenever(paymentRepository.save(anyVararg(Payment::class))).thenAnswer { it.arguments[0] }

        // when
        val result = paymentService.createPayment(group, paymentCreation)

        // then
        result.also {
            it.id.shouldNotBeNull()
            it.groupId shouldBe GROUP_ID
            it.creatorId shouldBe USER_ID
            it.recipientId shouldBe paymentCreation.recipientId
            it.title shouldBe paymentCreation.title
            it.type shouldBe paymentCreation.type
            it.amount shouldBe paymentCreation.amount
            it.fxData.also { fxData ->
                fxData?.targetCurrency shouldBe paymentCreation.targetCurrency
                fxData?.exchangeRate shouldBe exchangeRate.value
            }
            it.createdAt.shouldNotBeNull()
            it.updatedAt.shouldNotBeNull()
            it.attachmentId shouldBe ATTACHMENT_ID
            it.status shouldBe PENDING
            it.history shouldHaveSize 1
            it.history.first().also { entry ->
                entry.createdAt.shouldNotBeNull()
                entry.paymentAction shouldBe CREATED
                entry.participantId shouldBe USER_ID
                entry.comment shouldBe paymentCreation.message
            }
        }

        verify(currencyManagerClient, times(1)).getAvailableCurrencies()
        verify(currencyManagerClient, times(1)).getExchangeRate(eq(CURRENCY_1), eq(CURRENCY_2), any())
        verify(attachmentStoreClient, times(1)).generateBlankAttachment(GROUP_ID, USER_ID)
        verify(paymentRepository, times(1)).save(anyVararg(Payment::class))
    }

    context("throw ValidatorsException cause:") {
        withData(
            nameFn = { it.first },
            Quadruple(
                RECIPIENT_IS_CREATOR,
                createPaymentCreation(creatorId = USER_ID, recipientId = USER_ID),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(
                RECIPIENT_NOT_GROUP_MEMBER,
                createPaymentCreation(recipientId = ANOTHER_USER_ID),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(
                BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES,
                createPaymentCreation(targetCurrency = null),
                arrayOf(),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(
                BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY,
                createPaymentCreation(amount = createAmount(currency = CURRENCY_1), targetCurrency = CURRENCY_1),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES, createPaymentCreation(), arrayOf(CURRENCY_1), arrayOf(CURRENCY_1, CURRENCY_2)),
            Quadruple(BASE_CURRENCY_NOT_AVAILABLE, createPaymentCreation(), arrayOf(CURRENCY_1, CURRENCY_2), arrayOf(CURRENCY_2)),

        ) { (expectedMessage, paymentCreation, groupCurrencies, availableCurrencies) ->
            // given
            val group = createGroup(currencies = createCurrencies(*groupCurrencies))
            whenever(currencyManagerClient.getAvailableCurrencies()).thenReturn(createCurrencies(*availableCurrencies))
            whenever(currencyManagerClient.getExchangeRate(CURRENCY_1, CURRENCY_2, Instant.ofEpochMilli(0L)))
                .thenReturn(createExchangeRate())

            // when & then
            shouldThrowWithMessage<ValidatorsException>("Failed validations: $expectedMessage") {
                paymentService.createPayment(group, paymentCreation)
            }
            verify(paymentRepository, times(0)).save(anyVararg(Payment::class))
        }
    }

    should("get payment") {
        // given
        val payment = createPayment()
        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(payment)

        // when
        val result = paymentService.getPayment(PAYMENT_ID, GROUP_ID)

        // then
        result shouldBe payment
        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
    }

    should("throw MissingPaymentException when there is no payment for given id & groupId") {
        // given
        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(null)

        // when & then
        shouldThrowExactly<MissingPaymentException> { paymentService.getPayment(PAYMENT_ID, GROUP_ID) }
        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
    }

    should("decide") {
        // given
        val payment = createPayment()
        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(payment)

        val paymentDecision = createPaymentDecision(userId = OTHER_USER_ID)

        // when
        paymentService.decide(paymentDecision)

        // then
        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
        verify(paymentRepository, times(1)).save(anyVararg(Payment::class))
    }

    should("throw MissingPaymentException when payment is not present") {
        // given
        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(null)
        val paymentDecision = createPaymentDecision()

        // when & then
        shouldThrowExactly<MissingPaymentException> { paymentService.decide(paymentDecision) }
        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
        verify(paymentRepository, times(0)).save(anyVararg(Payment::class))
    }

    should("throw ValidatorsException when user is not recipient") {
        // given
        val payment = createPayment()
        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(payment)
        val paymentDecision = createPaymentDecision(userId = USER_ID)

        // when & then
        shouldThrowWithMessage<ValidatorsException>("Failed validations: $USER_NOT_RECIPIENT") {
            paymentService.decide(paymentDecision)
        }

        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
        verify(paymentRepository, times(0)).save(anyVararg(Payment::class))
    }

    should("delete payment") {
        // given
        val payment = createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = USER_ID)
        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(payment)

        // when
        paymentService.deletePayment(PAYMENT_ID, GROUP_ID, USER_ID)

        // then
        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
        verify(paymentRepository, times(1)).delete(payment)
        verify(archivedPaymentRepository, times(1)).add(payment)
    }

    should("throw MissingPaymentException when payment does not exist") {
        // given
        val payment = createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = USER_ID)
        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(null)

        // when & then
        shouldThrowExactly<MissingPaymentException> { paymentService.deletePayment(PAYMENT_ID, GROUP_ID, USER_ID) }
        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
        verify(paymentRepository, times(0)).delete(payment)
        verify(archivedPaymentRepository, times(0)).add(payment)
    }

    should("throw PaymentDeletionAccessException when user is not payment Creator") {
        // given
        val payment = createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = OTHER_USER_ID)
        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(payment)

        // when & then
        shouldThrowWithMessage<ValidatorsException>("Failed validations: $USER_NOT_CREATOR") {
            paymentService.deletePayment(PAYMENT_ID, GROUP_ID, USER_ID)
        }
        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
        verify(paymentRepository, times(0)).delete(payment)
        verify(archivedPaymentRepository, times(0)).add(payment)
    }

    should("update payment") {
        // given
        val payment = createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = USER_ID)
        val paymentUpdate = createPaymentUpdate(amount = createAmount(value = BigDecimal(6)))

        val exchangeRate = createExchangeRate(EXCHANGE_RATE_VALUE)
        val group = createGroup(createGroupMembers(USER_ID, OTHER_USER_ID, ANOTHER_USER_ID), currencies = createCurrencies(CURRENCY_1, CURRENCY_2))
        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(payment)
        whenever(currencyManagerClient.getExchangeRate(eq(CURRENCY_1), eq(CURRENCY_2), any())).thenReturn(exchangeRate)
        whenever(currencyManagerClient.getAvailableCurrencies()).thenReturn(createCurrencies(CURRENCY_1, CURRENCY_2))
        whenever(paymentRepository.save(anyVararg(Payment::class))).doAnswer { it.arguments[0] as? Payment }

        // when & then
        val result = paymentService.updatePayment(group, paymentUpdate)
        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
        verify(currencyManagerClient, times(1)).getAvailableCurrencies()
        verify(paymentRepository, times(1)).save(anyVararg(Payment::class))

        result.also {
            it.id shouldBe PAYMENT_ID
            it.groupId shouldBe GROUP_ID
            it.creatorId shouldBe USER_ID
            it.title shouldBe paymentUpdate.title
            it.type shouldBe paymentUpdate.type
            it.amount shouldBe paymentUpdate.amount
            it.fxData.also { fxData ->
                fxData?.targetCurrency shouldBe paymentUpdate.targetCurrency
                fxData?.exchangeRate shouldBe exchangeRate.value
            }
            it.date shouldBe paymentUpdate.date
            it.createdAt shouldBe payment.createdAt
            it.updatedAt.shouldNotBeNull()
            it.attachmentId shouldBe payment.attachmentId
            it.recipientId shouldBe payment.recipientId
            it.status shouldBe PENDING
            it.history shouldContainAll payment.history
            it.history.last().also { history ->
                history.participantId shouldBe USER_ID
                history.createdAt.shouldNotBeNull()
                history.paymentAction shouldBe EDITED
                history.comment shouldBe paymentUpdate.message
            }
        }
    }

    should("throw MissingPaymentException when updating payment and payment does not exist") {
        // given
        val paymentUpdate = createPaymentUpdate(id = PAYMENT_ID, groupId = GROUP_ID, userId = USER_ID)
        val group = createGroup(currencies = createCurrencies(CURRENCY_1, CURRENCY_2))

        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(null)

        // when & then
        shouldThrowExactly<MissingPaymentException> { paymentService.updatePayment(group, paymentUpdate) }
        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
        verify(paymentRepository, times(0)).save(anyVararg(Payment::class))
    }

    context("throw ValidatorsException when updating exception cause:") {
        withData(
            nameFn = { it.first },

            Quadruple(
                BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES,
                createPaymentUpdate(targetCurrency = null),
                arrayOf(),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(
                BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY,
                createPaymentUpdate(amount = createAmount(currency = CURRENCY_1), targetCurrency = CURRENCY_1),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES, createPaymentUpdate(), arrayOf(CURRENCY_1), arrayOf(CURRENCY_1, CURRENCY_2)),
            Quadruple(BASE_CURRENCY_NOT_AVAILABLE, createPaymentUpdate(), arrayOf(CURRENCY_1, CURRENCY_2), arrayOf(CURRENCY_2)),
            Quadruple(
                USER_NOT_CREATOR,
                createPaymentUpdate(userId = OTHER_USER_ID),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(
                NO_MODIFICATION,
                createPaymentUpdateFromPayment(createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = USER_ID)),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),

        ) { (expectedMessage, paymentUpdate, groupCurrencies, availableCurrencies) ->
            // given
            val payment = createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = USER_ID)
            val group = createGroup(createGroupMembers(USER_ID, OTHER_USER_ID, ANOTHER_USER_ID), currencies = createCurrencies(*groupCurrencies))
            whenever(currencyManagerClient.getAvailableCurrencies()).thenReturn(createCurrencies(*availableCurrencies))
            whenever(currencyManagerClient.getExchangeRate(CURRENCY_1, CURRENCY_2, Instant.ofEpochMilli(0L))).thenReturn(createExchangeRate())
            whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(payment)

            // when & then
            shouldThrowWithMessage<ValidatorsException>("Failed validations: $expectedMessage") { paymentService.updatePayment(group, paymentUpdate) }
            verify(paymentRepository, times(0)).save(anyVararg(Payment::class))
        }
    }

    should("get group activities") {
        // given
        val payments = listOf(createPayment())
        val filterOptions = createFilterOptions()
        whenever(paymentRepository.findByGroupId(GROUP_ID, filterOptions)).thenReturn(payments)

        // when
        val result = paymentService.getGroupActivities(GROUP_ID, filterOptions)

        // then
        result shouldBe payments
        verify(paymentRepository, times(1)).findByGroupId(GROUP_ID, filterOptions)
    }

    should("return empty list when group has no payments") {
        // given
        val filterOptions = createFilterOptions()

        whenever(paymentRepository.findByGroupId(GROUP_ID, filterOptions)).thenReturn(listOf())

        // when
        val result = paymentService.getGroupActivities(GROUP_ID, filterOptions)

        // then
        result shouldBe listOf()
        verify(paymentRepository, times(1)).findByGroupId(GROUP_ID, filterOptions)
    }

    should("get accepted payments") {
        // given
        val acceptedPayment = createPayment(status = ACCEPTED)
        whenever(paymentRepository.findByGroupId(eq(GROUP_ID), any())).thenReturn(listOf(acceptedPayment))

        // when
        val result = paymentService.getAcceptedGroupPayments(GROUP_ID)

        // then
        result.also {
            it shouldHaveSize 1
            it.first() shouldBe acceptedPayment
        }
        verify(paymentRepository, times(1)).findByGroupId(eq(GROUP_ID), any())
    }

    should("get user balance") {
        // given
        val payments = listOf(
            createPayment(
                status = ACCEPTED,
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
            createPayment(
                status = ACCEPTED,
                creatorId = OTHER_USER_ID,
                recipientId = USER_ID,
                amount = createAmount(
                    value = 50.toBigDecimal(),
                    currency = CURRENCY_1,
                ),
                fxData = null,
            ),
            createPayment(
                status = ACCEPTED,
                creatorId = OTHER_USER_ID,
                recipientId = ANOTHER_USER_ID,
            ),

        )
        whenever(paymentRepository.findByGroupId(eq(GROUP_ID), any())).thenReturn(payments)

        // when
        val result = paymentService.getUserBalance(GROUP_ID, USER_ID)

        // then
        result.also {
            it shouldHaveSize 2
            it.first().also { elem ->
                elem.value shouldBe payments.first().amount.value
                elem.currency shouldBe payments.first().fxData?.targetCurrency
                elem.exchangeRate shouldBe payments.first().fxData?.exchangeRate
            }
            it.last().also { elem ->
                elem.value shouldBe payments[1].amount.value.negate()
                elem.currency shouldBe payments[1].amount.currency
                elem.exchangeRate.shouldBeNull()
            }
        }
        verify(paymentRepository, times(1)).findByGroupId(eq(GROUP_ID), any())
    }
},)

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
