package pl.edu.agh.gem.internal.service

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.user.DummyUser.OTHER_USER_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.internal.client.AttachmentStoreClient
import pl.edu.agh.gem.internal.client.CurrencyManagerClient
import pl.edu.agh.gem.internal.client.GroupManagerClient
import pl.edu.agh.gem.internal.model.attachment.GroupAttachment
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentAction.CREATED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.persistence.PaymentRepository
import pl.edu.agh.gem.util.DummyData.ANOTHER_USER_ID
import pl.edu.agh.gem.util.DummyData.ATTACHMENT_ID
import pl.edu.agh.gem.util.DummyData.CURRENCY_1
import pl.edu.agh.gem.util.DummyData.CURRENCY_2
import pl.edu.agh.gem.util.DummyData.PAYMENT_ID
import pl.edu.agh.gem.util.createAmount
import pl.edu.agh.gem.util.createCurrencies
import pl.edu.agh.gem.util.createExchangeRate
import pl.edu.agh.gem.util.createGroup
import pl.edu.agh.gem.util.createPayment
import pl.edu.agh.gem.util.createPaymentCreation
import pl.edu.agh.gem.util.createPaymentDecision
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_AVAILABLE
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_IS_CREATOR
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_NOT_GROUP_MEMBER
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES
import pl.edu.agh.gem.validator.ValidatorsException
import java.time.Instant

class PaymentServiceTest : ShouldSpec({
    val groupManagerClient = mock<GroupManagerClient> { }
    val currencyManagerClient = mock<CurrencyManagerClient> {}
    val attachmentStoreClient = mock<AttachmentStoreClient> {}
    val paymentRepository = mock<PaymentRepository> {}

    val paymentService = PaymentService(
        groupManagerClient,
        currencyManagerClient,
        attachmentStoreClient,
        paymentRepository,
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

    should("throw PaymentRecipientDecisionException when payment is not present") {
        // given
        val payment = createPayment()
        whenever(paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)).thenReturn(payment)
        val paymentDecision = createPaymentDecision(userId = USER_ID)

        // when & then
        shouldThrowExactly<PaymentRecipientDecisionException> { paymentService.decide(paymentDecision) }
        verify(paymentRepository, times(1)).findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID)
        verify(paymentRepository, times(0)).save(anyVararg(Payment::class))
    }
},)

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
