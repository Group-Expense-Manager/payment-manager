package pl.edu.agh.gem.external.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.edu.agh.gem.exception.UserWithoutGroupAccessException
import pl.edu.agh.gem.external.dto.payment.PaymentCreationRequest
import pl.edu.agh.gem.external.dto.payment.PaymentDecisionRequest
import pl.edu.agh.gem.external.dto.payment.PaymentResponse
import pl.edu.agh.gem.external.dto.payment.PaymentUpdateRequest
import pl.edu.agh.gem.external.dto.payment.toPaymentResponse
import pl.edu.agh.gem.internal.client.GroupManagerClient
import pl.edu.agh.gem.internal.service.PaymentService
import pl.edu.agh.gem.media.InternalApiMediaType.APPLICATION_JSON_INTERNAL_VER_1
import pl.edu.agh.gem.model.GroupMembers
import pl.edu.agh.gem.paths.Paths.EXTERNAL
import pl.edu.agh.gem.security.GemUserId

@RestController
@RequestMapping("$EXTERNAL/payments")
class ExternalPaymentController(
    private val paymentService: PaymentService,
    private val groupManagerClient: GroupManagerClient,

) {

    @PostMapping(consumes = [APPLICATION_JSON_INTERNAL_VER_1], produces = [APPLICATION_JSON_INTERNAL_VER_1])
    @ResponseStatus(CREATED)
    fun createPayment(
        @GemUserId userId: String,
        @RequestParam groupId: String,
        @Valid @RequestBody
        paymentCreationRequest: PaymentCreationRequest,
    ): PaymentResponse {
        val group = paymentService.getGroup(groupId)
        userId.checkIfUserHaveAccess(group.members)

        return paymentService.createPayment(group, paymentCreationRequest.toDomain(userId, groupId)).toPaymentResponse()
    }

    @GetMapping("{paymentId}/groups/{groupId}", produces = [APPLICATION_JSON_INTERNAL_VER_1])
    @ResponseStatus(OK)
    fun getPayment(
        @GemUserId userId: String,
        @PathVariable paymentId: String,
        @PathVariable groupId: String,
    ): PaymentResponse {
        userId.checkIfUserHaveAccess(groupId)
        return paymentService.getPayment(paymentId, groupId).toPaymentResponse()
    }

    @PostMapping("decide", consumes = [APPLICATION_JSON_INTERNAL_VER_1])
    @ResponseStatus(OK)
    fun decide(
        @GemUserId userId: String,
        @Valid @RequestBody
        paymentDecisionRequest: PaymentDecisionRequest,
    ): PaymentResponse {
        userId.checkIfUserHaveAccess(paymentDecisionRequest.groupId)
        return paymentService.decide(paymentDecisionRequest.toDomain(userId)).toPaymentResponse()
    }

    @DeleteMapping("{paymentId}/groups/{groupId}")
    @ResponseStatus(OK)
    fun deletePayment(
        @GemUserId userId: String,
        @PathVariable paymentId: String,
        @PathVariable groupId: String,
    ) {
        userId.checkIfUserHaveAccess(groupId)
        paymentService.deletePayment(paymentId, groupId, userId)
    }

    @PutMapping("{paymentId}/groups/{groupId}", consumes = [APPLICATION_JSON_INTERNAL_VER_1], produces = [APPLICATION_JSON_INTERNAL_VER_1])
    @ResponseStatus(OK)
    fun updatePayment(
        @GemUserId userId: String,
        @PathVariable paymentId: String,
        @PathVariable groupId: String,
        @Valid @RequestBody
        paymentUpdateRequest: PaymentUpdateRequest,
    ): PaymentResponse {
        val group = paymentService.getGroup(groupId)
        userId.checkIfUserHaveAccess(group.members)
        return paymentService.updatePayment(group, paymentUpdateRequest.toDomain(paymentId, groupId, userId)).toPaymentResponse()
    }

    private fun String.checkIfUserHaveAccess(groupMembers: GroupMembers) {
        groupMembers.members.find { it.id == this } ?: throw UserWithoutGroupAccessException(this)
    }

    private fun String.checkIfUserHaveAccess(groupId: String) =
        groupManagerClient.getUserGroups(this).find { it.groupId == groupId } ?: throw UserWithoutGroupAccessException(this)
}
