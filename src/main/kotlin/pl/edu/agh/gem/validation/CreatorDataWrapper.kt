package pl.edu.agh.gem.validation

import pl.edu.agh.gem.validator.DataWrapper

interface CreatorDataWrapper : DataWrapper {
    val creatorData: CreatorData
}
