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
import pl.edu.agh.gem.validation.CurrenciesValidator
import pl.edu.agh.gem.validation.CurrencyData
import pl.edu.agh.gem.validation.creation.PaymentCreationDataWrapper
import pl.edu.agh.gem.validation.creation.RecipientValidator
import pl.edu.agh.gem.validation.decision.DecisionValidator
import pl.edu.agh.gem.validation.decision.PaymentDecisionDataWrapper
import pl.edu.agh.gem.validation.update.PaymentUpdateDataWrapper
import pl.edu.agh.gem.validator.ValidatorsException
import pl.edu.agh.gem.validator.alsoValidate
import pl.edu.agh.gem.validator.validate
import java.math.BigDecimal
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

        if (!userId.isCreator(paymentToDelete)) {
            throw PaymentDeletionAccessException(userId, paymentId)
        }

        paymentRepository.delete(paymentToDelete)
        archivedPaymentRepository.add(paymentToDelete)
    }

    private fun String.isCreator(payment: Payment) = payment.creatorId == this

    fun updatePayment(groupData: GroupData, update: PaymentUpdate): Payment {
        val originalPayment = paymentRepository.findByPaymentIdAndGroupId(update.id, update.groupId)
            ?: throw MissingPaymentException(update.id, update.groupId)

        if (!update.userId.isCreator(originalPayment)) {
            throw PaymentUpdateAccessException(update.userId, update.id)
        }

        if (!update.modifies(originalPayment)) {
            throw NoPaymentUpdateException(update.userId, update.id)
        }

        val partiallyUpdatedPayment = originalPayment.update(update)

        val dataWrapper = PaymentUpdateDataWrapper(
            CurrencyData(
                groupData.currencies,
                currencyManagerClient.getAvailableCurrencies(),
                update.amount.currency,
                update.targetCurrency,
            ),
        )

        validate(dataWrapper, currenciesValidator)
            .takeIf { it.isNotEmpty() }
            ?.also { throw ValidatorsException(it) }

        return paymentRepository.save(
            partiallyUpdatedPayment.copy(
                fxData = getFxData(
                    update.amount.currency,
                    update.targetCurrency,
                    update.date,
                ),
            ),
        )
    }

    private fun PaymentUpdate.modifies(payment: Payment): Boolean {
        return payment.title != title ||
            payment.type != type ||
            payment.amount != amount ||
            payment.fxData?.targetCurrency != targetCurrency ||
            payment.date != date
    }

    private fun Payment.update(paymentUpdate: PaymentUpdate): Payment {
        return this.copy(
            title = paymentUpdate.title,
            type = paymentUpdate.type,
            amount = paymentUpdate.amount,
            fxData = paymentUpdate.targetCurrency?.let {
                FxData(
                    paymentUpdate.targetCurrency,
                    BigDecimal.ZERO,
                )
            },
            date = paymentUpdate.date,
            updatedAt = now(),
            status = PENDING,
            history = history + PaymentHistoryEntry(creatorId, EDITED, now(), paymentUpdate.message),

        )
    }
}

class MissingPaymentException(paymentId: String, groupId: String) :
    RuntimeException("Failed to find payment with id: $paymentId and groupId: $groupId")

class PaymentDeletionAccessException(userId: String, paymentId: String) :
    RuntimeException("User with id: $userId can not delete payment with id: $paymentId")

class PaymentUpdateAccessException(userId: String, paymentId: String) :
    RuntimeException("User with id: $userId can not update payment with id: $paymentId")

class NoPaymentUpdateException(userId: String, paymentId: String) :
    RuntimeException("No update occurred for payment with id: $paymentId by user with id: $userId")
