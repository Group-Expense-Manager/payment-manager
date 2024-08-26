package pl.edu.agh.gem.internal.model.payment.filter

import pl.edu.agh.gem.internal.model.payment.PaymentStatus

data class FilterOptions(
    val title: String?,
    val status: PaymentStatus?,
    val creatorId: String?,
    val sortedBy: SortedBy,
    val sortOrder: SortOrder,
)
