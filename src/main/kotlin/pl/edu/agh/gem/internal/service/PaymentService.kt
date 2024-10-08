package pl.edu.agh.gem.internal.service

import org.springframework.stereotype.Service
import pl.edu.agh.gem.internal.client.CurrencyManagerClient
import pl.edu.agh.gem.internal.client.GroupManagerClient
import pl.edu.agh.gem.internal.mapper.BalanceElementMapper
import pl.edu.agh.gem.internal.model.group.GroupData
import pl.edu.agh.gem.internal.model.payment.BalanceElement
import pl.edu.agh.gem.internal.model.payment.FxData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentAction.EDITED
import pl.edu.agh.gem.internal.model.payment.PaymentCreation
import pl.edu.agh.gem.internal.model.payment.PaymentDecision
import pl.edu.agh.gem.internal.model.payment.PaymentHistoryEntry
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.ACCEPTED
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.model.payment.PaymentUpdate
import pl.edu.agh.gem.internal.model.payment.filter.FilterOptions
import pl.edu.agh.gem.internal.model.payment.filter.SortOrder.ASCENDING
import pl.edu.agh.gem.internal.model.payment.filter.SortedBy.DATE
import pl.edu.agh.gem.internal.persistence.ArchivedPaymentRepository
import pl.edu.agh.gem.internal.persistence.PaymentRepository
import pl.edu.agh.gem.validation.CreatorData
import pl.edu.agh.gem.validation.CreatorValidator
import pl.edu.agh.gem.validation.CurrenciesValidator
import pl.edu.agh.gem.validation.CurrencyData
import pl.edu.agh.gem.validation.creation.PaymentCreationDataWrapper
import pl.edu.agh.gem.validation.creation.RecipientValidator
import pl.edu.agh.gem.validation.decision.DecisionValidator
import pl.edu.agh.gem.validation.decision.PaymentDecisionDataWrapper
import pl.edu.agh.gem.validation.delete.PaymentDeletionDataWrapper
import pl.edu.agh.gem.validation.update.PaymentUpdateDataWrapper
import pl.edu.agh.gem.validator.ValidatorsException
import pl.edu.agh.gem.validator.alsoValidate
import pl.edu.agh.gem.validator.validate
import java.time.Instant.now
import java.time.ZoneId

@Service
class PaymentService(
    private val groupManagerClient: GroupManagerClient,
    private val currencyManagerClient: CurrencyManagerClient,
    private val paymentRepository: PaymentRepository,
    private val archivedPaymentRepository: ArchivedPaymentRepository,
) {

    val recipientValidator = RecipientValidator()
    val currenciesValidator = CurrenciesValidator()
    val decisionValidator = DecisionValidator()
    val creatorValidator = CreatorValidator()

    val balanceElementMapper = BalanceElementMapper()

    val acceptedPaymentsFilterOptions = FilterOptions(
        status = ACCEPTED,
        sortedBy = DATE,
        sortOrder = ASCENDING,
    )

    fun getGroup(groupId: String): GroupData {
        return groupManagerClient.getGroup(groupId)
    }

    fun createPayment(groupData: GroupData, paymentCreation: PaymentCreation): Payment {
        val dataWrapper = createPaymentCreationDataWrapper(groupData, paymentCreation)
        validate(dataWrapper, recipientValidator)
            .alsoValidate(dataWrapper, currenciesValidator)
            .takeIf { it.isNotEmpty() }
            ?.also { throw ValidatorsException(it) }

        val fxData = createFxData(paymentCreation)

        return paymentRepository.save(
            paymentCreation.toPayment(
                fxData = fxData,
            ),
        )
    }

    private fun createPaymentCreationDataWrapper(groupData: GroupData, paymentCreation: PaymentCreation): PaymentCreationDataWrapper {
        return PaymentCreationDataWrapper(
            groupData.members,
            paymentCreation,
            CurrencyData(
                groupData.currencies,
                currencyManagerClient.getAvailableCurrencies(),
                paymentCreation.amount.currency,
                paymentCreation.targetCurrency,
            ),
        )
    }

    fun getPayment(paymentId: String, groupId: String): Payment {
        return paymentRepository.findByPaymentIdAndGroupId(paymentId, groupId) ?: throw MissingPaymentException(paymentId, groupId)
    }

    private fun createFxData(paymentCreation: PaymentCreation) =
        paymentCreation.targetCurrency?.let {
            FxData(
                targetCurrency = paymentCreation.targetCurrency,
                exchangeRate = currencyManagerClient.getExchangeRate(
                    paymentCreation.amount.currency,
                    paymentCreation.targetCurrency,
                    paymentCreation.date.atZone(ZoneId.systemDefault()).toLocalDate(),
                ).value,
            )
        }

    fun decide(paymentDecision: PaymentDecision): Payment {
        val payment = paymentRepository.findByPaymentIdAndGroupId(paymentDecision.paymentId, paymentDecision.groupId)
            ?: throw MissingPaymentException(paymentDecision.paymentId, paymentDecision.groupId)

        val dataWrapper = PaymentDecisionDataWrapper(paymentDecision, payment)
        validate(dataWrapper, decisionValidator)
            .takeIf { it.isNotEmpty() }
            ?.also { throw ValidatorsException(it) }

        return paymentRepository.save(payment.addDecision(paymentDecision))
    }

    private fun Payment.addDecision(paymentDecision: PaymentDecision): Payment {
        val paymentHistoryEntry = PaymentHistoryEntry(
            participantId = paymentDecision.userId,
            paymentAction = paymentDecision.decision.toPaymentAction(),
            comment = paymentDecision.message,
        )
        val updatedHistory = history + paymentHistoryEntry

        return copy(
            updatedAt = now(),
            status = paymentDecision.decision.toPaymentStatus(),
            history = updatedHistory,
        )
    }

    fun deletePayment(paymentId: String, groupId: String, userId: String) {
        val paymentToDelete = paymentRepository.findByPaymentIdAndGroupId(paymentId, groupId) ?: throw MissingPaymentException(paymentId, groupId)

        val dataWrapper = PaymentDeletionDataWrapper(
            creatorData = CreatorData(
                creatorId = paymentToDelete.creatorId,
                userId = userId,
            ),
        )
        validate(dataWrapper, creatorValidator)
            .takeIf { it.isNotEmpty() }
            ?.also { throw ValidatorsException(it) }

        paymentRepository.delete(paymentToDelete)
        archivedPaymentRepository.add(paymentToDelete)
    }

    fun updatePayment(groupData: GroupData, update: PaymentUpdate): Payment {
        val originalPayment = paymentRepository.findByPaymentIdAndGroupId(update.id, update.groupId)
            ?: throw MissingPaymentException(update.id, update.groupId)

        val dataWrapper = createPaymentUpdateDataWrapper(
            originalPayment = originalPayment,
            paymentUpdate = update,
            groupData = groupData,
        )

        validate(dataWrapper, creatorValidator)
            .alsoValidate(dataWrapper, currenciesValidator)
            .takeIf { it.isNotEmpty() }
            ?.also { throw ValidatorsException(it) }

        return paymentRepository.save(
            originalPayment.copy(
                title = update.title,
                type = update.type,
                amount = update.amount,
                fxData = updateFxData(originalPayment = originalPayment, paymentUpdate = update),
                date = update.date,
                updatedAt = now(),
                status = PENDING,
                history = originalPayment.history + PaymentHistoryEntry(originalPayment.creatorId, EDITED, now(), update.message),
                attachmentId = update.attachmentId,
            ),
        )
    }

    private fun updateFxData(originalPayment: Payment, paymentUpdate: PaymentUpdate): FxData? {
        if (shouldUseOriginalFxData(originalPayment, paymentUpdate)) {
            return originalPayment.fxData
        }
        return paymentUpdate.targetCurrency?.let {
            FxData(
                targetCurrency = paymentUpdate.targetCurrency,
                exchangeRate = currencyManagerClient.getExchangeRate(
                    paymentUpdate.amount.currency,
                    paymentUpdate.targetCurrency,
                    paymentUpdate.date.atZone(ZoneId.systemDefault()).toLocalDate(),
                ).value,
            )
        }
    }

    private fun shouldUseOriginalFxData(originalPayment: Payment, update: PaymentUpdate): Boolean {
        return originalPayment.date == update.date && originalPayment.amount.currency == update.amount.currency &&
            originalPayment.fxData?.targetCurrency == update.targetCurrency
    }

    private fun createPaymentUpdateDataWrapper(
        originalPayment: Payment,
        paymentUpdate: PaymentUpdate,
        groupData: GroupData,
    ) = PaymentUpdateDataWrapper(
        originalPayment = originalPayment,
        paymentUpdate = paymentUpdate,
        currencyData = CurrencyData(
            groupData.currencies,
            currencyManagerClient.getAvailableCurrencies(),
            paymentUpdate.amount.currency,
            paymentUpdate.targetCurrency,
        ),
        creatorData = CreatorData(
            creatorId = originalPayment.creatorId,
            userId = paymentUpdate.userId,
        ),
    )

    fun getGroupActivities(groupId: String, filterOptions: FilterOptions): List<Payment> {
        return paymentRepository.findByGroupId(groupId, filterOptions)
    }

    fun getAcceptedGroupPayments(groupId: String): List<Payment> {
        return paymentRepository.findByGroupId(groupId, acceptedPaymentsFilterOptions)
    }

    fun getUserBalance(groupId: String, userId: String): List<BalanceElement> {
        return paymentRepository.findByGroupId(groupId, acceptedPaymentsFilterOptions)
            .mapNotNull { balanceElementMapper.mapToBalanceElement(userId = userId, payment = it) }
    }
}

class MissingPaymentException(paymentId: String, groupId: String) :
    RuntimeException("Failed to find payment with id: $paymentId and groupId: $groupId")
