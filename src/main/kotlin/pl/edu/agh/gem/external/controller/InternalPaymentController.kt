package pl.edu.agh.gem.external.controller

import org.springframework.http.HttpStatus.OK
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import pl.edu.agh.gem.external.dto.payment.AcceptedGroupPaymentsResponse
import pl.edu.agh.gem.external.dto.payment.GroupActivitiesResponse
import pl.edu.agh.gem.external.dto.payment.toAcceptedGroupPaymentsResponse
import pl.edu.agh.gem.external.dto.payment.toGroupActivitiesResponse
import pl.edu.agh.gem.internal.model.payment.PaymentStatus
import pl.edu.agh.gem.internal.model.payment.filter.FilterOptions
import pl.edu.agh.gem.internal.model.payment.filter.SortOrder
import pl.edu.agh.gem.internal.model.payment.filter.SortedBy
import pl.edu.agh.gem.internal.service.PaymentService
import pl.edu.agh.gem.media.InternalApiMediaType.APPLICATION_JSON_INTERNAL_VER_1
import pl.edu.agh.gem.paths.Paths.INTERNAL

@RestController
@RequestMapping("$INTERNAL/payments")
class InternalPaymentController(
    private val paymentService: PaymentService,
) {

    @GetMapping("activities/groups/{groupId}", produces = [APPLICATION_JSON_INTERNAL_VER_1])
    @ResponseStatus(OK)
    fun getGroupActivities(
        @PathVariable groupId: String,
        @RequestParam title: String?,
        @RequestParam status: PaymentStatus?,
        @RequestParam creatorId: String?,
        @RequestParam sortedBy: SortedBy,
        @RequestParam sortOrder: SortOrder,
    ): GroupActivitiesResponse {
        val filterOptions = FilterOptions(title, status, creatorId, sortedBy, sortOrder)
        return paymentService.getGroupActivities(groupId, filterOptions).toGroupActivitiesResponse(groupId)
    }

    @GetMapping("accepted/groups/{groupId}", produces = [APPLICATION_JSON_INTERNAL_VER_1])
    @ResponseStatus(OK)
    fun getAcceptedGroupPayments(
        @PathVariable groupId: String,
    ): AcceptedGroupPaymentsResponse {
        return paymentService.getAcceptedPayments(groupId).toAcceptedGroupPaymentsResponse(groupId)
    }
}
