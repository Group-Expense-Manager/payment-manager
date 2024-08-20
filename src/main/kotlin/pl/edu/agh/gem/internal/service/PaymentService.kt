package pl.edu.agh.gem.internal.service

import org.springframework.stereotype.Service
import pl.edu.agh.gem.internal.client.AttachmentStoreClient
import pl.edu.agh.gem.internal.client.CurrencyManagerClient
import pl.edu.agh.gem.internal.client.GroupManagerClient
import pl.edu.agh.gem.internal.model.group.GroupData
import pl.edu.agh.gem.internal.model.payment.FxData
import pl.edu.agh.gem.internal.model.payment.Payment
import pl.edu.agh.gem.internal.model.payment.PaymentAction.EDITED
import pl.edu.agh.gem.internal.model.payment.PaymentCreation
import pl.edu.agh.gem.internal.model.payment.PaymentDecision
import pl.edu.agh.gem.internal.model.payment.PaymentHistoryEntry
import pl.edu.agh.gem.internal.model.payment.PaymentStatus.PENDING
import pl.edu.agh.gem.internal.model.payment.PaymentUpdate
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
import pl.edu.agh.gem.validation.update.ModificationValidator
import pl.edu.agh.gem.validation.update.PaymentUpdateDataWrapper
import pl.edu.agh.gem.validator.ValidatorsException
import pl.edu.agh.gem.validator.alsoValidate
import pl.edu.agh.gem.validator.validate
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

    val recipientValidator = RecipientValidator()
    val currenciesValidator = CurrenciesValidator()
    val decisionValidator = DecisionValidator()
    val creatorValidator = CreatorValidator()
    val modificationValidator = ModificationValidator()

    fun getGroup(groupId: String): GroupData {
        return groupManagerClient.getGroup(groupId)
    }

    fun createPayment(groupData: GroupData, paymentCreation: PaymentCreation): Payment {
        val dataWrapper = createPaymentCreationDataWrapper(groupData, paymentCreation)
        validate(dataWrapper, recipientValidator)
            .alsoValidate(dataWrapper, currenciesValidator)
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

        val dataWrapper = PaymentDecisionDataWrapper(paymentDecision, payment)
        validate(dataWrapper, decisionValidator)
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
            .alsoValidate(dataWrapper, modificationValidator)
            .alsoValidate(dataWrapper, currenciesValidator)
            .takeIf { it.isNotEmpty() }
            ?.also { throw ValidatorsException(it) }

        return paymentRepository.save(
            originalPayment.copy(
                title = update.title,
                type = update.type,
                amount = update.amount,
                fxData = getFxData(
                    update.amount.currency,
                    update.targetCurrency,
                    update.date,
                ),
                date = update.date,
                updatedAt = now(),
                status = PENDING,
                history = originalPayment.history + PaymentHistoryEntry(originalPayment.creatorId, EDITED, now(), update.message),

            ),
        )
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
}

class MissingPaymentException(paymentId: String, groupId: String) :
    RuntimeException("Failed to find payment with id: $paymentId and groupId: $groupId")
