package pl.edu.agh.gem.integration.controler

import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.OK
import pl.edu.agh.gem.assertion.shouldBody
import pl.edu.agh.gem.assertion.shouldHaveErrors
import pl.edu.agh.gem.assertion.shouldHaveHttpStatus
import pl.edu.agh.gem.assertion.shouldHaveValidationError
import pl.edu.agh.gem.assertion.shouldHaveValidatorError
import pl.edu.agh.gem.exception.UserWithoutGroupAccessException
import pl.edu.agh.gem.external.dto.group.CurrencyDTO
import pl.edu.agh.gem.external.dto.payment.PaymentResponse
import pl.edu.agh.gem.external.dto.payment.toAmountDto
import pl.edu.agh.gem.external.dto.payment.toDto
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.group.DummyGroup.OTHER_GROUP_ID
import pl.edu.agh.gem.helper.user.DummyUser.EMAIL
import pl.edu.agh.gem.helper.user.DummyUser.OTHER_USER_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.helper.user.createGemUser
import pl.edu.agh.gem.integration.BaseIntegrationSpec
import pl.edu.agh.gem.integration.ability.ServiceTestClient
import pl.edu.agh.gem.integration.ability.stubAttachmentStoreGenerateBlankAttachment
import pl.edu.agh.gem.integration.ability.stubCurrencyManagerAvailableCurrencies
import pl.edu.agh.gem.integration.ability.stubCurrencyManagerExchangeRate
import pl.edu.agh.gem.integration.ability.stubGroupManagerGroupData
import pl.edu.agh.gem.integration.ability.stubGroupManagerUserGroups
import pl.edu.agh.gem.internal.model.payment.PaymentAction
import pl.edu.agh.gem.internal.model.payment.PaymentAction.EDITED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.ACCEPTED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.persistence.PaymentRepository
import pl.edu.agh.gem.internal.service.MissingPaymentException
import pl.edu.agh.gem.internal.service.Quadruple
import pl.edu.agh.gem.util.DummyData.ANOTHER_USER_ID
import pl.edu.agh.gem.util.DummyData.CURRENCY_1
import pl.edu.agh.gem.util.DummyData.CURRENCY_2
import pl.edu.agh.gem.util.DummyData.EXCHANGE_RATE_VALUE
import pl.edu.agh.gem.util.DummyData.PAYMENT_ID
import pl.edu.agh.gem.util.createAmountDto
import pl.edu.agh.gem.util.createCurrenciesDTO
import pl.edu.agh.gem.util.createCurrenciesResponse
import pl.edu.agh.gem.util.createExchangeRateResponse
import pl.edu.agh.gem.util.createGroupAttachmentResponse
import pl.edu.agh.gem.util.createGroupResponse
import pl.edu.agh.gem.util.createMembersDTO
import pl.edu.agh.gem.util.createPayment
import pl.edu.agh.gem.util.createPaymentCreationRequest
import pl.edu.agh.gem.util.createPaymentDecisionRequest
import pl.edu.agh.gem.util.createPaymentUpdateRequest
import pl.edu.agh.gem.util.createPaymentUpdateRequestFromPayment
import pl.edu.agh.gem.util.createUserGroupsResponse
import pl.edu.agh.gem.validation.ValidationMessage.AMOUNT_DECIMAL_PLACES
import pl.edu.agh.gem.validation.ValidationMessage.ATTACHMENT_ID_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_AVAILABLE
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES
import pl.edu.agh.gem.validation.ValidationMessage.BASE_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.GROUP_ID_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.MAX_AMOUNT
import pl.edu.agh.gem.validation.ValidationMessage.MESSAGE_NULL_OR_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.PAYMENT_ID_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.POSITIVE_AMOUNT
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_ID_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_IS_CREATOR
import pl.edu.agh.gem.validation.ValidationMessage.RECIPIENT_NOT_GROUP_MEMBER
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES
import pl.edu.agh.gem.validation.ValidationMessage.TARGET_CURRENCY_PATTERN
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_MAX_LENGTH
import pl.edu.agh.gem.validation.ValidationMessage.TITLE_NOT_BLANK
import pl.edu.agh.gem.validation.ValidationMessage.USER_NOT_CREATOR
import pl.edu.agh.gem.validation.ValidationMessage.USER_NOT_RECIPIENT
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId

class ExternalPaymentControllerIT(
    private val service: ServiceTestClient,
    private val paymentRepository: PaymentRepository,
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
            Instant.ofEpochSecond(0L).atZone(ZoneId.systemDefault()).toLocalDate(),
        )

        // when
        val response = service.createPayment(paymentCreationRequest, createGemUser(USER_ID), GROUP_ID)

        // then
        response shouldHaveHttpStatus CREATED
        response.shouldBody<PaymentResponse> {
            paymentId.shouldNotBeNull()
            creatorId shouldBe USER_ID
            recipientId shouldBe paymentCreationRequest.recipientId
            title shouldBe paymentCreationRequest.title
            type shouldBe paymentCreationRequest.type.name
            amount shouldBe paymentCreationRequest.amount
            fxData?.also { fxData ->
                fxData.targetCurrency shouldBe paymentCreationRequest.targetCurrency
                fxData.exchangeRate.shouldNotBeNull()
            }
            date shouldBe paymentCreationRequest.date
            createdAt.shouldNotBeNull()
            updatedAt.shouldNotBeNull()
            attachmentId shouldBe paymentCreationRequest.attachmentId
            status shouldBe PENDING.name
            history.first().also {
                it.createdAt.shouldNotBeNull()
                it.participantId shouldBe USER_ID
                it.paymentAction shouldBe PaymentAction.CREATED.name
                it.comment shouldBe paymentCreationRequest.message
            }
        }
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
            Instant.ofEpochSecond(0L).atZone(ZoneId.systemDefault()).toLocalDate(),
        )
        stubAttachmentStoreGenerateBlankAttachment(attachment, GROUP_ID, USER_ID)

        // when
        val response = service.createPayment(paymentCreationRequest, createGemUser(USER_ID), GROUP_ID)

        // then
        response shouldHaveHttpStatus CREATED
        response.shouldBody<PaymentResponse> {
            paymentId.shouldNotBeNull()
            creatorId shouldBe USER_ID
            recipientId shouldBe paymentCreationRequest.recipientId
            title shouldBe paymentCreationRequest.title
            type shouldBe paymentCreationRequest.type.name
            amount shouldBe paymentCreationRequest.amount
            fxData?.also { fxData ->
                fxData.targetCurrency shouldBe paymentCreationRequest.targetCurrency
                fxData.exchangeRate.shouldNotBeNull()
            }
            date shouldBe paymentCreationRequest.date
            createdAt.shouldNotBeNull()
            updatedAt.shouldNotBeNull()
            attachmentId.shouldNotBeNull()
            status shouldBe PENDING.name
            history.first().also {
                it.createdAt.shouldNotBeNull()
                it.participantId shouldBe USER_ID
                it.paymentAction shouldBe PaymentAction.CREATED.name
                it.comment shouldBe paymentCreationRequest.message
            }
        }
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
            Pair(MAX_AMOUNT, createPaymentCreationRequest(amount = createAmountDto(value = "100000".toBigDecimal()))),
            Pair(AMOUNT_DECIMAL_PLACES, createPaymentCreationRequest(amount = createAmountDto(value = "1000.001".toBigDecimal()))),
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
                Instant.ofEpochSecond(0L).atZone(ZoneId.systemDefault()).toLocalDate(),
            )

            // when
            val response = service.createPayment(paymentCreationRequest, createGemUser(), GROUP_ID)

            // then
            response shouldHaveHttpStatus BAD_REQUEST
            response shouldHaveValidatorError expectedMessage
        }
    }

    should("get payment") {
        // given
        stubGroupManagerUserGroups(createUserGroupsResponse(GROUP_ID, OTHER_GROUP_ID), USER_ID)
        val payment = createPayment(PAYMENT_ID)
        paymentRepository.save(payment)

        // when
        val response = service.getPayment(createGemUser(USER_ID), PAYMENT_ID, GROUP_ID)

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<PaymentResponse> {
            paymentId shouldBe payment.id
            creatorId shouldBe payment.creatorId
            recipientId shouldBe payment.recipientId
            title shouldBe payment.title
            type shouldBe payment.type.name
            amount shouldBe payment.amount.toAmountDto()
            fxData shouldBe payment.fxData?.toDto()
            date shouldBe payment.date
            createdAt.shouldNotBeNull()
            updatedAt.shouldNotBeNull()
            attachmentId shouldBe payment.attachmentId
            status shouldBe payment.status.name
            history.first().also {
                it.createdAt.shouldNotBeNull()
                it.participantId shouldBe payment.history.first().participantId
                it.paymentAction shouldBe payment.history.first().paymentAction.name
                it.comment shouldBe payment.history.first().comment
            }
        }
    }

    should("return forbidden if user is not a group member") {
        // given
        stubGroupManagerUserGroups(createUserGroupsResponse(OTHER_GROUP_ID), USER_ID)

        // when
        val response = service.getPayment(createGemUser(USER_ID), PAYMENT_ID, GROUP_ID)

        // then
        response shouldHaveHttpStatus FORBIDDEN
    }

    should("return not found when payment doesn't exist") {
        // given
        stubGroupManagerUserGroups(createUserGroupsResponse(GROUP_ID, OTHER_GROUP_ID), USER_ID)

        // when
        val response = service.getPayment(createGemUser(USER_ID), PAYMENT_ID, GROUP_ID)

        // then
        response shouldHaveHttpStatus NOT_FOUND
    }

    should("decide") {
        // given
        val decisionRequest = createPaymentDecisionRequest()
        stubGroupManagerUserGroups(createUserGroupsResponse(GROUP_ID, OTHER_GROUP_ID), OTHER_USER_ID)

        val payment = createPayment()
        paymentRepository.save(payment)

        // when
        val response = service.decide(decisionRequest, createGemUser(id = OTHER_USER_ID))

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<PaymentResponse> {
            paymentId shouldBe PAYMENT_ID
            creatorId shouldBe USER_ID
            title shouldBe payment.title
            type shouldBe payment.type.name
            amount shouldBe payment.amount.toAmountDto()
            fxData shouldBe payment.fxData?.toDto()
            date shouldBe payment.date
            createdAt shouldBe payment.createdAt
            updatedAt.shouldNotBeNull()
            attachmentId shouldBe payment.attachmentId
            recipientId shouldBe payment.recipientId
            status shouldBe ACCEPTED.name
            history.last().also { history ->
                history.participantId shouldBe OTHER_USER_ID
                history.createdAt.shouldNotBeNull()
                history.paymentAction shouldBe PaymentAction.ACCEPTED.name
                history.comment shouldBe decisionRequest.message
            }
        }
    }

    context("return validation exception when decide cause:") {
        withData(
            nameFn = { it.first },
            Pair(PAYMENT_ID_NOT_BLANK, createPaymentDecisionRequest(paymentId = "")),
            Pair(GROUP_ID_NOT_BLANK, createPaymentDecisionRequest(groupId = "")),
            Pair(MESSAGE_NULL_OR_NOT_BLANK, createPaymentDecisionRequest(message = "")),

        ) { (expectedMessage, paymentDecisionRequest) ->
            // when
            val response = service.decide(paymentDecisionRequest, createGemUser())

            // then
            response shouldHaveHttpStatus BAD_REQUEST
            response shouldHaveValidationError expectedMessage
        }
    }

    should("return forbidden if user is not a group member") {
        // given
        val decisionRequest = createPaymentDecisionRequest()
        stubGroupManagerUserGroups(createUserGroupsResponse(OTHER_GROUP_ID), USER_ID)

        // when
        val response = service.decide(decisionRequest, createGemUser(id = USER_ID))

        // then
        response shouldHaveHttpStatus FORBIDDEN
        response shouldHaveErrors {
            errors shouldHaveSize 1
            errors.first().code shouldBe UserWithoutGroupAccessException::class.simpleName
        }
    }

    should("return not found when payment is not present") {
        // given
        val decisionRequest = createPaymentDecisionRequest()
        stubGroupManagerUserGroups(createUserGroupsResponse(GROUP_ID, OTHER_GROUP_ID), USER_ID)

        // when
        val response = service.decide(decisionRequest, createGemUser(id = USER_ID))

        // then
        response shouldHaveHttpStatus NOT_FOUND
        response shouldHaveErrors {
            errors shouldHaveSize 1
            errors.first().code shouldBe MissingPaymentException::class.simpleName
        }
    }

    should("return forbidden if user is not an payment recipient") {
        // given
        val decisionRequest = createPaymentDecisionRequest()
        stubGroupManagerUserGroups(createUserGroupsResponse(GROUP_ID, OTHER_GROUP_ID), USER_ID)

        val payment = createPayment()
        paymentRepository.save(payment)

        // when
        val response = service.decide(decisionRequest, createGemUser(id = USER_ID))

        // then
        response shouldHaveHttpStatus BAD_REQUEST
        response shouldHaveValidatorError USER_NOT_RECIPIENT
    }

    should("delete payment") {
        // given
        val payment = createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = USER_ID)
        paymentRepository.save(payment)
        stubGroupManagerUserGroups(createUserGroupsResponse(GROUP_ID, OTHER_GROUP_ID), USER_ID)

        // when
        val response = service.delete(createGemUser(USER_ID, EMAIL), GROUP_ID, PAYMENT_ID)

        // then
        response shouldHaveHttpStatus OK
        paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID).also {
            it.shouldBeNull()
        }
    }
    should("return forbidden if user is not a group member") {
        // given
        stubGroupManagerUserGroups(createUserGroupsResponse(OTHER_GROUP_ID), USER_ID)

        // when
        val response = service.delete(createGemUser(USER_ID, EMAIL), GROUP_ID, PAYMENT_ID)

        // then
        response shouldHaveHttpStatus FORBIDDEN
        response shouldHaveErrors {
            errors shouldHaveSize 1
            errors.first().code shouldBe UserWithoutGroupAccessException::class.simpleName
        }
    }

    should("return not found when payment is not present") {
        // given
        stubGroupManagerUserGroups(createUserGroupsResponse(GROUP_ID, OTHER_GROUP_ID), USER_ID)

        // when
        val response = service.delete(createGemUser(USER_ID, EMAIL), GROUP_ID, PAYMENT_ID)

        // then
        response shouldHaveHttpStatus NOT_FOUND
        response shouldHaveErrors {
            errors shouldHaveSize 1
            errors.first().code shouldBe MissingPaymentException::class.simpleName
        }
    }

    should("return forbidden when user is not payment creator") {
        // given
        val payment = createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = OTHER_USER_ID)
        paymentRepository.save(payment)
        stubGroupManagerUserGroups(createUserGroupsResponse(GROUP_ID, OTHER_GROUP_ID), USER_ID)

        // when
        val response = service.delete(createGemUser(USER_ID, EMAIL), GROUP_ID, PAYMENT_ID)

        // then
        response shouldHaveHttpStatus BAD_REQUEST
        response shouldHaveValidatorError USER_NOT_CREATOR
    }

    context("return validation exception when updating payment cause:") {
        withData(
            nameFn = { it.first },
            Pair(TITLE_NOT_BLANK, createPaymentUpdateRequest(title = "")),
            Pair(
                TITLE_MAX_LENGTH,
                createPaymentUpdateRequest(title = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
            ),
            Pair(POSITIVE_AMOUNT, createPaymentUpdateRequest(amount = createAmountDto(value = BigDecimal.ZERO))),
            Pair(MAX_AMOUNT, createPaymentUpdateRequest(amount = createAmountDto(value = "100000".toBigDecimal()))),
            Pair(AMOUNT_DECIMAL_PLACES, createPaymentUpdateRequest(amount = createAmountDto(value = "1000.001".toBigDecimal()))),
            Pair(BASE_CURRENCY_NOT_BLANK, createPaymentUpdateRequest(amount = createAmountDto(currency = ""))),
            Pair(BASE_CURRENCY_PATTERN, createPaymentUpdateRequest(amount = createAmountDto(currency = "pln"))),
            Pair(TARGET_CURRENCY_PATTERN, createPaymentUpdateRequest(targetCurrency = "pln")),
            Pair(MESSAGE_NULL_OR_NOT_BLANK, createPaymentUpdateRequest(message = "")),
        ) { (expectedMessage, paymentUpdateRequest) ->
            // when
            val response = service.updatePayment(paymentUpdateRequest, createGemUser(), GROUP_ID, PAYMENT_ID)

            // then
            response shouldHaveHttpStatus BAD_REQUEST
            response shouldHaveValidationError expectedMessage
        }
    }

    should("not update payment when user doesn't have access") {
        // given
        val user = createGemUser()
        val paymentUpdateRequest = createPaymentUpdateRequest()
        stubGroupManagerGroupData(createGroupResponse(members = createMembersDTO(OTHER_USER_ID)), GROUP_ID)

        // when
        val response = service.updatePayment(paymentUpdateRequest, user, GROUP_ID, PAYMENT_ID)

        // then
        response shouldHaveHttpStatus FORBIDDEN
    }

    should("return not found when updating payment and payment is not present") {
        // given
        val paymentUpdateRequest = createPaymentUpdateRequest()
        stubGroupManagerGroupData(createGroupResponse(members = createMembersDTO(USER_ID, OTHER_USER_ID)), GROUP_ID)

        // when
        val response = service.updatePayment(paymentUpdateRequest, createGemUser(USER_ID), GROUP_ID, PAYMENT_ID)

        // then
        response shouldHaveHttpStatus NOT_FOUND
        response shouldHaveErrors {
            errors shouldHaveSize 1
            errors.first().code shouldBe MissingPaymentException::class.simpleName
        }
    }
    context("return validator exception when updating exception cause:") {
        withData(
            nameFn = { it.first },
            Quintuple(
                BASE_CURRENCY_NOT_IN_GROUP_CURRENCIES,
                createPaymentUpdateRequest(targetCurrency = null),
                listOf(),
                arrayOf(CURRENCY_1, CURRENCY_2),
                USER_ID,
            ),
            Quintuple(
                BASE_CURRENCY_EQUAL_TO_TARGET_CURRENCY,
                createPaymentUpdateRequest(amount = createAmountDto(currency = CURRENCY_1), targetCurrency = CURRENCY_1),
                listOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_1, CURRENCY_2),
                USER_ID,
            ),
            Quintuple(
                TARGET_CURRENCY_NOT_IN_GROUP_CURRENCIES,
                createPaymentUpdateRequest(),
                listOf(CURRENCY_1),
                arrayOf(CURRENCY_1, CURRENCY_2),
                USER_ID,
            ),
            Quintuple(
                BASE_CURRENCY_NOT_AVAILABLE,
                createPaymentUpdateRequest(),
                listOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_2),
                USER_ID,
            ),
            Quintuple(
                USER_NOT_CREATOR,
                createPaymentUpdateRequest(),
                listOf(CURRENCY_1, CURRENCY_2),
                arrayOf(CURRENCY_2),
                OTHER_USER_ID,
            ),

        ) { (expectedMessage, updatePaymentRequest, groupCurrencies, availableCurrencies, creatorId) ->

            // given
            val payment = createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = creatorId)
            paymentRepository.save(payment)
            stubGroupManagerGroupData(
                createGroupResponse(createMembersDTO(USER_ID, OTHER_USER_ID), groupCurrencies = groupCurrencies.map { CurrencyDTO(it) }),
                GROUP_ID,
            )
            stubCurrencyManagerAvailableCurrencies(createCurrenciesResponse(*availableCurrencies))

            // when
            val response = service.updatePayment(updatePaymentRequest, createGemUser(USER_ID), GROUP_ID, PAYMENT_ID)

            // then
            response shouldHaveHttpStatus BAD_REQUEST
            response shouldHaveValidatorError expectedMessage
        }
    }

    should("update payment") {
        // given
        val payment = createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = USER_ID)
        val paymentUpdateRequest = createPaymentUpdateRequest(amount = createAmountDto(value = "6".toBigDecimal()))
        paymentRepository.save(payment)
        stubGroupManagerGroupData(createGroupResponse(members = createMembersDTO(USER_ID, OTHER_USER_ID, ANOTHER_USER_ID)), GROUP_ID)
        stubCurrencyManagerAvailableCurrencies(createCurrenciesResponse(CURRENCY_1, CURRENCY_2))
        stubCurrencyManagerExchangeRate(
            createExchangeRateResponse(value = EXCHANGE_RATE_VALUE),
            CURRENCY_1,
            CURRENCY_2,
            Instant.ofEpochSecond(0L).atZone(ZoneId.systemDefault()).toLocalDate(),
        )
        // when
        val response = service.updatePayment(paymentUpdateRequest, createGemUser(USER_ID), GROUP_ID, PAYMENT_ID)

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<PaymentResponse> {
            paymentId.shouldNotBeNull()
            creatorId shouldBe USER_ID
            recipientId shouldBe payment.recipientId
            title shouldBe paymentUpdateRequest.title
            type shouldBe paymentUpdateRequest.type.name
            amount shouldBe paymentUpdateRequest.amount
            fxData?.also { fxData ->
                fxData.targetCurrency shouldBe paymentUpdateRequest.targetCurrency
                fxData.exchangeRate.shouldNotBeNull()
            }
            date shouldBe paymentUpdateRequest.date
            createdAt.shouldNotBeNull()
            updatedAt.shouldNotBeNull()
            attachmentId.shouldNotBeNull()
            status shouldBe PENDING.name
            history.last().also {
                it.createdAt.shouldNotBeNull()
                it.participantId shouldBe USER_ID
                it.paymentAction shouldBe EDITED.name
                it.comment shouldBe paymentUpdateRequest.message
            }
        }
        paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID).also {
            it.shouldNotBeNull()
            it.id shouldBe PAYMENT_ID
            it.groupId shouldBe GROUP_ID
            it.creatorId shouldBe USER_ID
            it.title shouldBe paymentUpdateRequest.title
            it.type shouldBe paymentUpdateRequest.type
            it.amount shouldBe paymentUpdateRequest.amount.toDomain()
            it.fxData.also { fxData ->
                fxData?.targetCurrency shouldBe paymentUpdateRequest.targetCurrency
                fxData?.exchangeRate shouldBe EXCHANGE_RATE_VALUE
            }
            it.createdAt.shouldNotBeNull()
            it.updatedAt.shouldNotBeNull()
            it.attachmentId shouldBe payment.attachmentId
            it.recipientId shouldBe payment.recipientId
            it.status shouldBe PENDING
            it.history.last().also { history ->
                history.participantId shouldBe USER_ID
                history.createdAt.shouldNotBeNull()
                history.paymentAction shouldBe EDITED
                history.comment shouldBe paymentUpdateRequest.message
            }
        }
    }

    should("update payment when data did not change") {
        // given
        val payment = createPayment(id = PAYMENT_ID, groupId = GROUP_ID, creatorId = USER_ID)
        val paymentUpdateRequest = createPaymentUpdateRequestFromPayment(payment)
        paymentRepository.save(payment)
        stubGroupManagerGroupData(createGroupResponse(members = createMembersDTO(USER_ID, OTHER_USER_ID, ANOTHER_USER_ID)), GROUP_ID)
        stubCurrencyManagerAvailableCurrencies(createCurrenciesResponse(CURRENCY_1, CURRENCY_2))
        stubCurrencyManagerExchangeRate(
            createExchangeRateResponse(value = EXCHANGE_RATE_VALUE),
            CURRENCY_1,
            CURRENCY_2,
            Instant.ofEpochSecond(0L).atZone(ZoneId.systemDefault()).toLocalDate(),
        )
        // when
        val response = service.updatePayment(paymentUpdateRequest, createGemUser(USER_ID), GROUP_ID, PAYMENT_ID)

        // then
        response shouldHaveHttpStatus OK
        response.shouldBody<PaymentResponse> {
            paymentId.shouldNotBeNull()
            creatorId shouldBe USER_ID
            recipientId shouldBe payment.recipientId
            title shouldBe paymentUpdateRequest.title
            type shouldBe paymentUpdateRequest.type.name
            amount shouldBe paymentUpdateRequest.amount
            fxData?.also { fxData ->
                fxData.targetCurrency shouldBe paymentUpdateRequest.targetCurrency
                fxData.exchangeRate.shouldNotBeNull()
            }
            date shouldBe paymentUpdateRequest.date
            createdAt.shouldNotBeNull()
            updatedAt.shouldNotBeNull()
            attachmentId.shouldNotBeNull()
            status shouldBe PENDING.name
            history.last().also {
                it.createdAt.shouldNotBeNull()
                it.participantId shouldBe USER_ID
                it.paymentAction shouldBe EDITED.name
                it.comment shouldBe paymentUpdateRequest.message
            }
        }
        paymentRepository.findByPaymentIdAndGroupId(PAYMENT_ID, GROUP_ID).also {
            it.shouldNotBeNull()
            it.id shouldBe PAYMENT_ID
            it.groupId shouldBe GROUP_ID
            it.creatorId shouldBe USER_ID
            it.title shouldBe paymentUpdateRequest.title
            it.type shouldBe paymentUpdateRequest.type
            it.amount shouldBe paymentUpdateRequest.amount.toDomain()
            it.fxData.also { fxData ->
                fxData?.targetCurrency shouldBe paymentUpdateRequest.targetCurrency
                fxData?.exchangeRate shouldBe EXCHANGE_RATE_VALUE
            }
            it.createdAt.shouldNotBeNull()
            it.updatedAt.shouldNotBeNull()
            it.attachmentId shouldBe payment.attachmentId
            it.recipientId shouldBe payment.recipientId
            it.status shouldBe PENDING
            it.history.last().also { history ->
                history.participantId shouldBe USER_ID
                history.createdAt.shouldNotBeNull()
                history.paymentAction shouldBe EDITED
                history.comment shouldBe paymentUpdateRequest.message
            }
        }
    }
},)

data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)
