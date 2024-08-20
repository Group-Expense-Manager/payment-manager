package pl.edu.agh.gem.internal.service

import org.springframework.stereotype.Service
import pl.edu.agh.gem.internal.client.AttachmentStoreClient
import pl.edu.agh.gem.internal.client.CurrencyManagerClient
import pl.edu.agh.gem.internal.client.GroupManagerClient
import pl.edu.agh.gem.internal.model.group.GroupData
import pl.edu.agh.gem.internal.model.payment.FxData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentCreation
import pl.edu.agh.gem.internal.model.payment.PaymentDecision
import pl.edu.agh.gem.internal.model.payment.PaymentHistoryEntry
import pl.edu.agh.gem.internal.persistence.ArchivedPaymentRepository
import pl.edu.agh.gem.internal.persistence.PaymentRepository
import pl.edu.agh.gem.validation.creation.CurrenciesValidator
import pl.edu.agh.gem.validation.creation.PaymentCreationDataWrapper
import pl.edu.agh.gem.validation.creation.RecipientValidator
import pl.edu.agh.gem.validation.decision.DecisionDataWrapper
import pl.edu.agh.gem.validation.decision.DecisionValidator
import pl.edu.agh.gem.validator.ValidatorList.Companion.validatorsOf
import pl.edu.agh.gem.validator.ValidatorsException
import java.time.Instant
import java.time.Instant.now

@Service
class PaymentService(
    private val groupManagerClient: GroupManagerClient,
    private val currencyManagerClient: CurrencyManagerClient,
    private val attachmentStoreClient: AttachmentStoreClient,
    private val paymentRepository: PaymentRepository,
    private val archivedPaymentRepository: ArchivedPaymentRepository,
) {

    private val paymentCreationValidators = validatorsOf(
        RecipientValidator(),
        CurrenciesValidator(),
    )

    private val paymentDecisionValidators = validatorsOf(
        DecisionValidator(),
    )

    fun getGroup(groupId: String): GroupData {
        return groupManagerClient.getGroup(groupId)
    }

    fun createPayment(groupData: GroupData, paymentCreation: PaymentCreation): Payment {
        paymentCreationValidators
            .getFailedValidations(createPaymentCreationDataWrapper(groupData, paymentCreation))
            .takeIf { it.isNotEmpty() }
            ?.also { throw ValidatorsException(it) }

        val attachmentId = paymentCreation.attachmentId
            ?: attachmentStoreClient.generateBlankAttachment(paymentCreation.groupId, paymentCreation.creatorId).id

        return paymentRepository.save(
            paymentCreation.toPayment(
                fxData = getFxData(paymentCreation.amount.currency, paymentCreation.targetCurrency, paymentCreation.date),
                attachmentId = attachmentId,
            ),
        )
    }

    private fun createPaymentCreationDataWrapper(groupData: GroupData, paymentCreation: PaymentCreation): PaymentCreationDataWrapper {
        return PaymentCreationDataWrapper(
            groupData,
            paymentCreation,
            currencyManagerClient.getAvailableCurrencies(),
        )
    }

    fun getPayment(paymentId: String, groupId: String): Payment {
        return paymentRepository.findByPaymentIdAndGroupId(paymentId, groupId) ?: throw MissingPaymentException(paymentId, groupId)
    }

    private fun getFxData(baseCurrency: String, targetCurrency: String?, date: Instant) =
        targetCurrency?.let {
            FxData(
                targetCurrency = targetCurrency,
                exchangeRate = currencyManagerClient.getExchangeRate(baseCurrency, targetCurrency, date).value,
            )
        }
    fun decide(paymentDecision: PaymentDecision) {
        val payment = paymentRepository.findByPaymentIdAndGroupId(paymentDecision.paymentId, paymentDecision.groupId)
            ?: throw MissingPaymentException(paymentDecision.paymentId, paymentDecision.groupId)

        paymentDecisionValidators
            .getFailedValidations(DecisionDataWrapper(paymentDecision, payment))
            .takeIf { it.isNotEmpty() }
            ?.also { throw ValidatorsException(it) }

        paymentRepository.save(payment.addDecision(paymentDecision))
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

        if (!userId.isCreator(paymentToDelete)) {
            throw PaymentDeletionAccessException(userId, paymentId)
        }

        paymentRepository.delete(paymentToDelete)
        archivedPaymentRepository.add(paymentToDelete)
    }

    private fun String.isCreator(payment: Payment) = payment.creatorId == this
}

class MissingPaymentException(paymentId: String, groupId: String) :
    RuntimeException("Failed to find payment with id: $paymentId and groupId: $groupId")

class PaymentDeletionAccessException(userId: String, paymentId: String) :
    RuntimeException("User with id: $userId can not delete payment with id: $paymentId")
