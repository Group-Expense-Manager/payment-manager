package pl.edu.agh.gem.integration.controler

import io.kotest.datatest.withData
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import pl.edu.agh.gem.assertion.shouldHaveHttpStatus
import pl.edu.agh.gem.assertion.shouldHaveValidationError
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.user.DummyUser.OTHER_USER_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.helper.user.createGemUser
import pl.edu.agh.gem.integration.BaseIntegrationSpec
import pl.edu.agh.gem.integration.ability.ServiceTestClient
import pl.edu.agh.gem.integration.ability.stubCurrencyManagerAvailableCurrencies
import pl.edu.agh.gem.integration.ability.stubCurrencyManagerExchangeRate
import pl.edu.agh.gem.integration.ability.stubGroupManagerGroupData
import pl.edu.agh.gem.util.DummyData.CURRENCY_1
import pl.edu.agh.gem.util.DummyData.CURRENCY_2
import pl.edu.agh.gem.util.DummyData.EXCHANGE_RATE_VALUE
import pl.edu.agh.gem.util.createCurrenciesDTO
import pl.edu.agh.gem.util.createCurrenciesResponse
import pl.edu.agh.gem.util.createExchangeRateResponse
import pl.edu.agh.gem.util.createGroupResponse
import pl.edu.agh.gem.util.createMembersDTO
import pl.edu.agh.gem.util.createPaymentCreationRequest
import pl.edu.agh.gem.validation.ValidationMessage.ATTACHMENT_ID_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.MESSAGE_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.POSITIVE_SUM
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_ID_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_MAX_LENGTH
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_NOT_BLANK
import java.math.BigDecimal

class ExternalPaymentControllerIT(
    private val service: ServiceTestClient,
) : BaseIntegrationSpec({

    should("create payment") {
        // given
        val paymentCreationRequest = createPaymentCreationRequest()
        stubGroupManagerGroupData(createGroupResponse(groupCurrencies = createCurrenciesDTO(CURRENCY_2)), GROUP_ID)
        stubCurrencyManagerAvailableCurrencies(createCurrenciesResponse(CURRENCY_1, CURRENCY_2))
        stubCurrencyManagerExchangeRate(
            createExchangeRateResponse(value = EXCHANGE_RATE_VALUE),
            CURRENCY_1,
            CURRENCY_2,
        )

        // when
        val response = service.createPayment(paymentCreationRequest, createGemUser(USER_ID), GROUP_ID)

        // then
        response shouldHaveHttpStatus CREATED
    }

    should("not create payment when user dont have access") {
        // given
        val user = createGemUser()
        val groupId = GROUP_ID
        val paymentCreationRequest = createPaymentCreationRequest()
        stubGroupManagerGroupData(createGroupResponse(members = createMembersDTO(OTHER_USER_ID)), GROUP_ID)

        // when
        val response = service.createPayment(paymentCreationRequest, user, groupId)

        // then
        response shouldHaveHttpStatus FORBIDDEN
    }

    context("return validation exception cause:") {
        withData(
            nameFn = { it.first },
            Pair(TITLE_NOT_BLANK, createPaymentCreationRequest(title = "")),
            Pair(
                TITLE_MAX_LENGTH,
                createPaymentCreationRequest(title = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
            ),
            Pair(POSITIVE_SUM, createPaymentCreationRequest(sum = BigDecimal.ZERO)),
            Pair(BASE_CURRENCY_NOT_BLANK, createPaymentCreationRequest(baseCurrency = "")),
            Pair(BASE_CURRENCY_PATTERN, createPaymentCreationRequest(baseCurrency = "pln")),
            Pair(TARGET_CURRENCY_PATTERN, createPaymentCreationRequest(targetCurrency = "pln")),
            Pair(RECIPIENT_ID_NOT_BLANK, createPaymentCreationRequest(recipientId = "")),
            Pair(MESSAGE_NULL_OR_NOT_BLANK, createPaymentCreationRequest(message = "")),
            Pair(ATTACHMENT_ID_NOT_BLANK, createPaymentCreationRequest(attachmentId = "")),

        ) { (expectedMessage, expenseCreationRequest) ->
            // when
            val response = service.createPayment(expenseCreationRequest, createGemUser(), GROUP_ID)

            // then
            response shouldHaveHttpStatus BAD_REQUEST
            response shouldHaveValidationError expectedMessage
        }
    }
},)
