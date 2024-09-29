package pl.edu.agh.gem.util

import pl.edu.agh.gem.external.dto.attachment.GroupAttachmentResponse
import pl.edu.agh.gem.external.dto.currency.CurrenciesResponse
import pl.edu.agh.gem.external.dto.currency.ExchangeRateResponse
import pl.edu.agh.gem.external.dto.group.CurrencyDTO
import pl.edu.agh.gem.external.dto.group.GroupDto
import pl.edu.agh.gem.external.dto.group.GroupResponse
import pl.edu.agh.gem.external.dto.group.MemberDTO
import pl.edu.agh.gem.external.dto.group.UserGroupsResponse
import pl.edu.agh.gem.external.dto.payment.AmountDto
import pl.edu.agh.gem.external.dto.payment.PaymentCreationRequest
import pl.edu.agh.gem.external.dto.payment.PaymentDecisionRequest
import pl.edu.agh.gem.external.dto.payment.PaymentUpdateRequest
import pl.edu.agh.gem.external.dto.payment.toAmountDto
import pl.edu.agh.gem.external.persistence.PaymentEntity
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.group.DummyGroup.OTHER_GROUP_ID
import pl.edu.agh.gem.helper.group.createGroupMembers
import pl.edu.agh.gem.helper.user.DummyUser.OTHER_USER_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.internal.model.currency.Currency
import pl.edu.agh.gem.internal.model.currency.ExchangeRate
import pl.edu.agh.gem.internal.model.group.GroupData
import pl.edu.agh.gem.internal.model.payment.Amount
import pl.edu.agh.gem.internal.model.payment.BalanceElement
import pl.edu.agh.gem.internal.model.payment.Decision
import pl.edu.agh.gem.internal.model.payment.Decision.ACCEPT
import pl.edu.agh.gem.internal.model.payment.FxData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentAction.CREATED
import pl.edu.agh.gem.internal.model.payment.PaymentCreation
import pl.edu.agh.gem.internal.model.payment.PaymentDecision
import pl.edu.agh.gem.internal.model.payment.PaymentHistoryEntry
import pl.edu.agh.gem.internal.model.payment.PaymentStatus
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.model.payment.PaymentType
import pl.edu.agh.gem.internal.model.payment.PaymentType.CASH
import pl.edu.agh.gem.internal.model.payment.PaymentType.OTHER
import pl.edu.agh.gem.internal.model.payment.PaymentUpdate
import pl.edu.agh.gem.internal.model.payment.filter.FilterOptions
import pl.edu.agh.gem.internal.model.payment.filter.SortOrder
import pl.edu.agh.gem.internal.model.payment.filter.SortOrder.ASCENDING
import pl.edu.agh.gem.internal.model.payment.filter.SortedBy
import pl.edu.agh.gem.internal.model.payment.filter.SortedBy.DATE
import pl.edu.agh.gem.model.GroupMembers
import pl.edu.agh.gem.util.DummyData.ATTACHMENT_ID
import pl.edu.agh.gem.util.DummyData.CURRENCY_1
import pl.edu.agh.gem.util.DummyData.CURRENCY_2
import pl.edu.agh.gem.util.DummyData.EXCHANGE_RATE_VALUE
import pl.edu.agh.gem.util.DummyData.PAYMENT_ID
import java.math.BigDecimal
import java.time.Instant
import java.time.Instant.now

fun createPaymentCreationRequest(
    title: String = "My Payment",
    type: PaymentType = CASH,
    amount: AmountDto = createAmountDto(),
    targetCurrency: String? = CURRENCY_2,
    date: Instant = Instant.ofEpochMilli(0L),
    recipientId: String = OTHER_USER_ID,
    message: String? = "Something",
    attachmentId: String? = ATTACHMENT_ID,
) = PaymentCreationRequest(
    title = title,
    type = type,
    amount = amount,
    targetCurrency = targetCurrency,
    date = date,
    recipientId = recipientId,
    message = message,
    attachmentId = attachmentId,
)

fun createAmountDto(
    value: BigDecimal = "10".toBigDecimal(),
    currency: String = CURRENCY_1,
) = AmountDto(
    value = value,
    currency = currency,
)

fun createPaymentCreation(
    groupId: String = GROUP_ID,
    creatorId: String = USER_ID,
    title: String = "My Payment",
    type: PaymentType = CASH,
    amount: Amount = createAmount(),
    targetCurrency: String? = CURRENCY_2,
    date: Instant = Instant.ofEpochMilli(0L),
    recipientId: String = OTHER_USER_ID,
    message: String? = "Something",
    attachmentId: String? = ATTACHMENT_ID,
) = PaymentCreation(
    creatorId = creatorId,
    groupId = groupId,
    title = title,
    type = type,
    amount = amount,
    targetCurrency = targetCurrency,
    date = date,
    recipientId = recipientId,
    message = message,
    attachmentId = attachmentId,
)

fun createPaymentEntity(
    id: String = PAYMENT_ID,
    groupId: String = GROUP_ID,
    creatorId: String = USER_ID,
    recipientId: String = OTHER_USER_ID,
    title: String = "My Payment",
    type: PaymentType = CASH,
    amount: Amount = createAmount(),
    fxData: FxData? = createFxData(),
    date: Instant = Instant.ofEpochMilli(0L),
    createdAt: Instant = Instant.ofEpochMilli(10),
    updatedAt: Instant = Instant.ofEpochMilli(20),
    attachmentId: String = ATTACHMENT_ID,
    status: PaymentStatus = PENDING,
    history: List<PaymentHistoryEntry> = arrayListOf(PaymentHistoryEntry(USER_ID, CREATED)),
) = PaymentEntity(
    id = id,
    groupId = groupId,
    creatorId = creatorId,
    recipientId = recipientId,
    title = title,
    type = type,
    amount = amount,
    fxData = fxData,
    date = date,
    createdAt = createdAt,
    updatedAt = updatedAt,
    attachmentId = attachmentId,
    status = status,
    history = history,
)

fun createAmount(
    value: BigDecimal = "10".toBigDecimal(),
    currency: String = CURRENCY_1,
) = Amount(
    value = value,
    currency = currency,
)

fun createFxData(
    targetCurrency: String = CURRENCY_2,
    exchangeRate: BigDecimal = EXCHANGE_RATE_VALUE,
) = FxData(
    targetCurrency = targetCurrency,
    exchangeRate = exchangeRate,
)

fun createPayment(
    id: String = PAYMENT_ID,
    groupId: String = GROUP_ID,
    creatorId: String = USER_ID,
    recipientId: String = OTHER_USER_ID,
    title: String = "My Payment",
    type: PaymentType = CASH,
    amount: Amount = createAmount(),
    fxData: FxData? = createFxData(),
    date: Instant = Instant.ofEpochMilli(0L),
    createdAt: Instant = Instant.ofEpochMilli(10),
    updatedAt: Instant = Instant.ofEpochMilli(20),
    attachmentId: String = ATTACHMENT_ID,
    status: PaymentStatus = PENDING,
    history: List<PaymentHistoryEntry> = arrayListOf(PaymentHistoryEntry(USER_ID, CREATED)),
) = Payment(
    id = id,
    groupId = groupId,
    creatorId = creatorId,
    recipientId = recipientId,
    title = title,
    type = type,
    amount = amount,
    fxData = fxData,
    date = date,
    createdAt = createdAt,
    updatedAt = updatedAt,
    attachmentId = attachmentId,
    status = status,
    history = history,
)
fun createCurrencies(
    vararg currencies: String = arrayOf(CURRENCY_1),
) = currencies.map { Currency(it) }

fun createCurrenciesResponse(
    vararg currencies: String = arrayOf(CURRENCY_1),
) = CurrenciesResponse(currencies.map { CurrencyDTO(it) })

fun createExchangeRate(
    value: BigDecimal = EXCHANGE_RATE_VALUE,
) = ExchangeRate(value)

fun createExchangeRateResponse(
    currencyFrom: String = CURRENCY_1,
    currencyTo: String = CURRENCY_2,
    value: BigDecimal = EXCHANGE_RATE_VALUE,
    createdAt: Instant = now(),
) = ExchangeRateResponse(
    currencyFrom = currencyFrom,
    currencyTo = currencyTo,
    rate = value,
    createdAt = createdAt,
)

fun createCurrenciesDTO(
    vararg currency: String = arrayOf(CURRENCY_1, CURRENCY_2),
) = currency.map { CurrencyDTO(it) }

fun createMembersDTO(
    vararg members: String = arrayOf(USER_ID, OTHER_USER_ID),
) = members.map { MemberDTO(it) }

fun createGroupResponse(
    members: List<MemberDTO> = listOf(USER_ID, OTHER_USER_ID).map { MemberDTO(it) },
    groupCurrencies: List<CurrencyDTO> = listOf(CURRENCY_1, CURRENCY_2).map { CurrencyDTO(it) },
) = GroupResponse(
    members = members,
    groupCurrencies = groupCurrencies,
)

fun createUserGroupsResponse(
    vararg groups: String = arrayOf(GROUP_ID, OTHER_GROUP_ID),
) = UserGroupsResponse(groups = groups.map { GroupDto(it) })

fun createGroup(
    members: GroupMembers = createGroupMembers(USER_ID, OTHER_USER_ID),
    currencies: List<Currency> = createCurrencies(CURRENCY_1, CURRENCY_2),
) = GroupData(
    members = members,
    currencies = currencies,
)

fun createGroupAttachmentResponse(
    attachmentId: String = ATTACHMENT_ID,
) = GroupAttachmentResponse(
    id = attachmentId,
)

fun createPaymentDecisionRequest(
    paymentId: String = PAYMENT_ID,
    groupId: String = GROUP_ID,
    decision: Decision = ACCEPT,
    message: String = "Some message",
) = PaymentDecisionRequest(
    paymentId = paymentId,
    groupId = groupId,
    decision = decision,
    message = message,
)

fun createPaymentDecision(
    userId: String = USER_ID,
    paymentId: String = PAYMENT_ID,
    groupId: String = GROUP_ID,
    decision: Decision = ACCEPT,
    message: String = "Some message",
) = PaymentDecision(
    userId = userId,
    paymentId = paymentId,
    groupId = groupId,
    decision = decision,
    message = message,
)

fun createPaymentUpdateRequest(
    title: String = "My Edited Payment",
    type: PaymentType = OTHER,
    amount: AmountDto = createAmountDto(),
    targetCurrency: String? = CURRENCY_2,
    date: Instant = Instant.ofEpochMilli(0L),
    message: String? = "edited",
) = PaymentUpdateRequest(
    title = title,
    type = type,
    amount = amount,
    targetCurrency = targetCurrency,
    date = date,
    message = message,
)

fun createPaymentUpdate(
    id: String = PAYMENT_ID,
    groupId: String = GROUP_ID,
    userId: String = USER_ID,
    title: String = "Some modified title",
    type: PaymentType = OTHER,
    amount: Amount = createAmount(),
    targetCurrency: String? = CURRENCY_2,
    date: Instant = Instant.ofEpochMilli(0L),
    message: String? = "Something",
) = PaymentUpdate(
    id = id,
    groupId = groupId,
    userId = userId,
    title = title,
    type = type,
    amount = amount,
    targetCurrency = targetCurrency,
    date = date,
    message = message,
)

fun createPaymentUpdateFromPayment(
    payment: Payment,
) = PaymentUpdate(
    id = payment.id,
    groupId = payment.groupId,
    userId = payment.creatorId,
    title = payment.title,
    type = payment.type,
    amount = payment.amount,
    targetCurrency = payment.fxData?.targetCurrency,
    date = payment.date,
    message = null,
)

fun createPaymentUpdateRequestFromPayment(
    payment: Payment,
) = PaymentUpdateRequest(
    title = payment.title,
    type = payment.type,
    amount = payment.amount.toAmountDto(),
    targetCurrency = payment.fxData?.targetCurrency,
    date = payment.date,
    message = null,
)

fun createFilterOptions(
    title: String? = null,
    status: PaymentStatus? = null,
    creatorId: String? = null,
    sortedBy: SortedBy = DATE,
    sortOrder: SortOrder = ASCENDING,
) = FilterOptions(
    title = title,
    status = status,
    creatorId = creatorId,
    sortedBy = sortedBy,
    sortOrder = sortOrder,
)
fun createBalanceElement(
    value: BigDecimal = BigDecimal.ONE,
    currency: String = CURRENCY_1,
    exchangeRate: BigDecimal? = null,
) = BalanceElement(
    value = value,
    currency = currency,
    exchangeRate = exchangeRate,
)

object DummyData {
    const val PAYMENT_ID = "paymentId"
    const val CURRENCY_1 = "PLN"
    const val CURRENCY_2 = "EUR"
    const val ATTACHMENT_ID = "attachmentId"
    const val ANOTHER_USER_ID = "anotherUserId"
    val EXCHANGE_RATE_VALUE: BigDecimal = BigDecimal.TWO
}

data class Pair<A, B>(
    val first: A,
    val second: B,
)
