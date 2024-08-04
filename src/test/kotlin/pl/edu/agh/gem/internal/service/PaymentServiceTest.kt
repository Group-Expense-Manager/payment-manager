package pl.edu.agh.gem.internal.service

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.internal.client.CurrencyManagerClient
import pl.edu.agh.gem.internal.client.GroupManagerClient
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.persistence.PaymentRepository
import pl.edu.agh.gem.util.DummyData.ANOTHER_USER_ID
import pl.edu.agh.gem.util.DummyData.CURRENCY_1
import pl.edu.agh.gem.util.DummyData.CURRENCY_2
import pl.edu.agh.gem.util.createCurrencies
import pl.edu.agh.gem.util.createExchangeRate
import pl.edu.agh.gem.util.createGroup
import pl.edu.agh.gem.util.createPayment
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
    val paymentRepository = mock<PaymentRepository> {}

    val paymentService = PaymentService(
        groupManagerClient,
        currencyManagerClient,
        paymentRepository,
    )

    should("create payment") {
        // given
        val payment = createPayment()
        val group = createGroup(currencies = createCurrencies(CURRENCY_1, CURRENCY_2))
        val exchangeRate = createExchangeRate()
        val expected = payment.copy(exchangeRate = exchangeRate.value)
        whenever(currencyManagerClient.getAvailableCurrencies()).thenReturn(createCurrencies(CURRENCY_1, CURRENCY_2))
        whenever(currencyManagerClient.getExchangeRate(eq(CURRENCY_1), eq(CURRENCY_2), any())).thenReturn(exchangeRate)
        whenever(paymentRepository.save(anyVararg(Payment::class))).thenReturn(expected)

        // when
        val result = paymentService.createPayment(group, payment)

        // then
        result shouldBe expected

        verify(currencyManagerClient, times(1)).getAvailableCurrencies()
        verify(currencyManagerClient, times(1)).getExchangeRate(eq(CURRENCY_1), eq(CURRENCY_2), any())
        verify(paymentRepository, times(1)).save(anyVararg(Payment::class))
    }

    context("throw ValidatorsException cause:") {
        withData(
            nameFn = { it.first },
            Quadruple(
                RECIPIENT_IS_CREATOR,
                createPayment(creatorId = USER_ID, recipientId = USER_ID),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(
                RECIPIENT_NOT_GROUP_MEMBER,
                createPayment(recipientId = ANOTHER_USER_ID),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES, createPayment(targetCurrency = null), arrayOf(), arrayOf(CURRENCY_1, CURRENCY_2)),
            Quadruple(
                BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY,
                createPayment(baseCurrency = CURRENCY_1, targetCurrency = CURRENCY_1),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES, createPayment(), arrayOf(CURRENCY_1), arrayOf(CURRENCY_1, CURRENCY_2)),
            Quadruple(BASE_CURRENCY_NOT_AVAILABLE, createPayment(), arrayOf(CURRENCY_1, CURRENCY_2), arrayOf(CURRENCY_2)),

        ) { (expectedMessage, expense, groupCurrencies, availableCurrencies) ->
            // given
            val group = createGroup(currencies = createCurrencies(*groupCurrencies))
            whenever(currencyManagerClient.getAvailableCurrencies()).thenReturn(createCurrencies(*availableCurrencies))
            whenever(currencyManagerClient.getExchangeRate(CURRENCY_1, CURRENCY_2, Instant.ofEpochMilli(0L))).thenReturn(createExchangeRate())

            // when & then
            shouldThrowWithMessage<ValidatorsException>("Failed validations: $expectedMessage") { paymentService.createPayment(group, expense) }
            verify(paymentRepository, times(0)).save(anyVararg(Payment::class))
        }
    }
},)

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
