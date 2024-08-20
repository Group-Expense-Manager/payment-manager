package pl.edu.agh.gem.validation.delete

import pl.edu.agh.gem.validation.CreatorData
import pl.edu.agh.gem.validation.CreatorDataWrapper

data class PaymentDeletionDataWrapper(
    override val creatorData: CreatorData,
) : CreatorDataWrapper
