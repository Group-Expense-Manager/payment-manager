package pl.edu.agh.gem.integration.controler

import io.kotest.datatest.withData
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import pl.edu.agh.gem.assertion.shouldHaveHttpStatus
import pl.edu.agh.gem.assertion.shouldHaveValidationError
import pl.edu.agh.gem.assertion.shouldHaveValidatorError
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.user.DummyUser.OTHER_USER_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.helper.user.createGemUser
import pl.edu.agh.gem.integration.BaseIntegrationSpec
import pl.edu.agh.gem.integration.ability.ServiceTestClient
import pl.edu.agh.gem.integration.ability.stubAttachmentStoreGenerateBlankAttachment
import pl.edu.agh.gem.integration.ability.stubCurrencyManagerAvailableCurrencies
import pl.edu.agh.gem.integration.ability.stubCurrencyManagerExchangeRate
import pl.edu.agh.gem.integration.ability.stubGroupManagerGroupData
import pl.edu.agh.gem.internal.service.Quadruple
import pl.edu.agh.gem.util.DummyData.ANOTHER_USER_ID
import pl.edu.agh.gem.util.DummyData.CURRENCY_1
import pl.edu.agh.gem.util.DummyData.CURRENCY_2
import pl.edu.agh.gem.util.DummyData.EXCHANGE_RATE_VALUE
import pl.edu.agh.gem.util.createAmountDto
import pl.edu.agh.gem.util.createCurrenciesDTO
import pl.edu.agh.gem.util.createCurrenciesResponse
import pl.edu.agh.gem.util.createExchangeRateResponse
import pl.edu.agh.gem.util.createGroupAttachmentResponse
import pl.edu.agh.gem.util.createGroupResponse
import pl.edu.agh.gem.util.createMembersDTO
import pl.edu.agh.gem.util.createPaymentCreationRequest
import pl.edu.agh.gem.validation.ValidationMessage.ATTACHMENT_ID_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_AVAILABLE
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.MESSAGE_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.POSITIVE_AMOUNT
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_ID_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_IS_CREATOR
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_NOT_GROUP_MEMBER
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_MAX_LENGTH
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_NOT_BLANK
import java.math.BigDecimal

class ExternalPaymentControllerIT(
    private val service: ServiceTestClient,
) : BaseIntegrationSpec({

    should("create payment when attachmentId is provided") {
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

    should("create payment when attachmentId is not provided") {
        // given
        val paymentCreationRequest = createPaymentCreationRequest(attachmentId = null)
        val attachment = createGroupAttachmentResponse()

        stubGroupManagerGroupData(createGroupResponse(groupCurrencies = createCurrenciesDTO(CURRENCY_2)), GROUP_ID)
        stubCurrencyManagerAvailableCurrencies(createCurrenciesResponse(CURRENCY_1, CURRENCY_2))
        stubCurrencyManagerExchangeRate(
            createExchangeRateResponse(value = EXCHANGE_RATE_VALUE),
            CURRENCY_1,
            CURRENCY_2,
        )
        stubAttachmentStoreGenerateBlankAttachment(attachment, GROUP_ID, USER_ID)

        // when
        val response = service.createPayment(paymentCreationRequest, createGemUser(USER_ID), GROUP_ID)

        // then
        response shouldHaveHttpStatus CREATED
    }

    should("return forbidden when user dont have access") {
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

    context("return bad request with cause:") {
        withData(
            nameFn = { it.first },
            Pair(TITLE_NOT_BLANK, createPaymentCreationRequest(title = "")),
            Pair(
                TITLE_MAX_LENGTH,
                createPaymentCreationRequest(title = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
            ),
            Pair(POSITIVE_AMOUNT, createPaymentCreationRequest(amount = createAmountDto(value = BigDecimal.ZERO))),
            Pair(BASE_CURRENCY_NOT_BLANK, createPaymentCreationRequest(amount = createAmountDto(currency = ""))),
            Pair(BASE_CURRENCY_PATTERN, createPaymentCreationRequest(amount = createAmountDto(currency = "pln"))),
            Pair(TARGET_CURRENCY_PATTERN, createPaymentCreationRequest(targetCurrency = "pln")),
            Pair(RECIPIENT_ID_NOT_BLANK, createPaymentCreationRequest(recipientId = "")),
            Pair(MESSAGE_NULL_OR_NOT_BLANK, createPaymentCreationRequest(message = "")),
            Pair(ATTACHMENT_ID_NULL_OR_NOT_BLANK, createPaymentCreationRequest(attachmentId = "")),

        ) { (expectedMessage, paymentCreationRequest) ->
            // when
            val response = service.createPayment(paymentCreationRequest, createGemUser(), GROUP_ID)

            // then
            response shouldHaveHttpStatus BAD_REQUEST
            response shouldHaveValidationError expectedMessage
        }
    }
    context("return forbidden  cause validator exception:") {
        withData(
            nameFn = { it.first },
            Quadruple(
                RECIPIENT_IS_CREATOR,
                createPaymentCreationRequest(recipientId = USER_ID),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(
                RECIPIENT_NOT_GROUP_MEMBER,
                createPaymentCreationRequest(recipientId = ANOTHER_USER_ID),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(
                BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES,
                createPaymentCreationRequest(targetCurrency = null),
                arrayOf(),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(
                BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY,
                createPaymentCreationRequest(amount = createAmountDto(currency = CURRENCY_1), targetCurrency = CURRENCY_1),
                arrayOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(
                TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES,
                createPaymentCreationRequest(),
                arrayOf(CURRENCY_1),
                arrayOf(CURRENCY_1, CURRENCY_2),
            ),
            Quadruple(BASE_CURRENCY_NOT_AVAILABLE, createPaymentCreationRequest(), arrayOf(CURRENCY_1, CURRENCY_2), arrayOf(CURRENCY_2)),

        ) { (expectedMessage, paymentCreationRequest, groupCurrencies, availableCurrencies) ->
            // given
            stubCurrencyManagerAvailableCurrencies(createCurrenciesResponse(*availableCurrencies))
            stubGroupManagerGroupData(createGroupResponse(groupCurrencies = createCurrenciesDTO(*groupCurrencies)), GROUP_ID)
            stubCurrencyManagerExchangeRate(
                createExchangeRateResponse(value = EXCHANGE_RATE_VALUE),
                CURRENCY_1,
                CURRENCY_2,
            )

            // when
            val response = service.createPayment(paymentCreationRequest, createGemUser(), GROUP_ID)

            // then
            response shouldHaveHttpStatus BAD_REQUEST
            response shouldHaveValidatorError expectedMessage
        }
    }
},)
