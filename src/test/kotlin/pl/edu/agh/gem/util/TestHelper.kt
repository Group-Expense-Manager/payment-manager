package pl.edu.agh.gem.util

import pl.edu.agh.gem.external.dto.currency.CurrenciesResponse
import pl.edu.agh.gem.external.dto.currency.ExchangeRateResponse
import pl.edu.agh.gem.external.dto.group.CurrencyDTO
import pl.edu.agh.gem.external.dto.group.GroupDto
import pl.edu.agh.gem.external.dto.group.GroupResponse
import pl.edu.agh.gem.external.dto.group.MemberDTO
import pl.edu.agh.gem.external.dto.group.UserGroupsResponse
import pl.edu.agh.gem.external.dto.payment.PaymentCreationRequest
import pl.edu.agh.gem.external.persistence.PaymentEntity
import pl.edu.agh.gem.helper.group.DummyGroup.GROUP_ID
import pl.edu.agh.gem.helper.group.DummyGroup.OTHER_GROUP_ID
import pl.edu.agh.gem.helper.group.createGroupMembers
import pl.edu.agh.gem.helper.user.DummyUser.OTHER_USER_ID
import pl.edu.agh.gem.helper.user.DummyUser.USER_ID
import pl.edu.agh.gem.internal.model.currency.Currency
import pl.edu.agh.gem.internal.model.currency.ExchangeRate
import pl.edu.agh.gem.internal.model.group.Currencies
import pl.edu.agh.gem.internal.model.group.GroupData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentAction.CREATED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.model.payment.PaymentType
import pl.edu.agh.gem.internal.model.payment.PaymentType.CASH
import pl.edu.agh.gem.internal.model.payment.StatusHistoryEntry
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
    sum: BigDecimal = "10".toBigDecimal(),
    baseCurrency: String = CURRENCY_1,
    targetCurrency: String? = CURRENCY_2,
    recipientId: String = OTHER_USER_ID,
    message: String? = "Something",
    attachmentId: String = ATTACHMENT_ID,
) = PaymentCreationRequest(
    title = title,
    type = type,
    sum = sum,
    baseCurrency = baseCurrency,
    targetCurrency = targetCurrency,
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
    sum: BigDecimal = "10".toBigDecimal(),
    baseCurrency: String = CURRENCY_1,
    targetCurrency: String? = CURRENCY_2,
    exchangeRate: BigDecimal? = EXCHANGE_RATE_VALUE,
    createdAt: Instant = Instant.ofEpochMilli(10),
    updatedAt: Instant = Instant.ofEpochMilli(20),
    attachmentId: String = ATTACHMENT_ID,
    status: PaymentStatus = PENDING,
    statusHistory: List<StatusHistoryEntry> = arrayListOf(StatusHistoryEntry(USER_ID, CREATED)),
) = PaymentEntity(
    id = id,
    groupId = groupId,
    creatorId = creatorId,
    recipientId = recipientId,
    title = title,
    type = type,
    sum = sum,
    baseCurrency = baseCurrency,
    targetCurrency = targetCurrency,
    exchangeRate = exchangeRate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    attachmentId = attachmentId,
    status = status,
    statusHistory = statusHistory,
)

fun createPayment(
    id: String = PAYMENT_ID,
    groupId: String = GROUP_ID,
    creatorId: String = USER_ID,
    recipientId: String = OTHER_USER_ID,
    title: String = "My Payment",
    type: PaymentType = CASH,
    sum: BigDecimal = "10".toBigDecimal(),
    baseCurrency: String = CURRENCY_1,
    targetCurrency: String? = CURRENCY_2,
    exchangeRate: BigDecimal? = EXCHANGE_RATE_VALUE,
    createdAt: Instant = Instant.ofEpochMilli(10),
    updatedAt: Instant = Instant.ofEpochMilli(20),
    attachmentId: String = ATTACHMENT_ID,
    status: PaymentStatus = PENDING,
    statusHistory: List<StatusHistoryEntry> = arrayListOf(StatusHistoryEntry(USER_ID, CREATED)),
) = Payment(
    id = id,
    groupId = groupId,
    creatorId = creatorId,
    recipientId = recipientId,
    title = title,
    type = type,
    sum = sum,
    baseCurrency = baseCurrency,
    targetCurrency = targetCurrency,
    exchangeRate = exchangeRate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    attachmentId = attachmentId,
    status = status,
    statusHistory = statusHistory,
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
    acceptRequired: Boolean = false,
    groupCurrencies: List<CurrencyDTO> = listOf(CURRENCY_1, CURRENCY_2).map { CurrencyDTO(it) },
) = GroupResponse(
    members = members,
    acceptRequired = acceptRequired,
    groupCurrencies = groupCurrencies,
)

fun createUserGroupsResponse(
    vararg groups: String = arrayOf(GROUP_ID, OTHER_GROUP_ID),
) = UserGroupsResponse(groups = groups.map { GroupDto(it) })

fun createGroup(
    members: GroupMembers = createGroupMembers(USER_ID, OTHER_USER_ID),
    acceptRequired: Boolean = false,
    currencies: Currencies = createCurrencies(CURRENCY_1, CURRENCY_2),
) = GroupData(
    members = members,
    acceptRequired = acceptRequired,
    currencies = currencies,
)

object DummyData {
    const val PAYMENT_ID = "paymentId"
    const val CURRENCY_1 = "PLN"
    const val CURRENCY_2 = "EUR"
    const val ATTACHMENT_ID = "attachmentId"
    const val ANOTHER_USER_ID = "anotherUserId"
    val EXCHANGE_RATE_VALUE: BigDecimal = BigDecimal.TWO
}
