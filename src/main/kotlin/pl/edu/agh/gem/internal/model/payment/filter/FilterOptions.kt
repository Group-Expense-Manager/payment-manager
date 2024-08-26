package pl.edu.agh.gem.internal.model.payment.filter

import pl.edu.agh.gem.internal.model.payment.PaymentStatus

data class FilterOptions(
    val title: String? = null,
    val status: PaymentStatus? = null,
    val creatorId: String? = null,
    val sortedBy: SortedBy,
    val sortOrder: SortOrder,
)
