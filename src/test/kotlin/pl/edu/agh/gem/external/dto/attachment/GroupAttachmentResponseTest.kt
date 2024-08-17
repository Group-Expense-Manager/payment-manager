package pl.edu.agh.gem.external.dto.attachment

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import pl.edu.agh.gem.util.createGroupAttachmentResponse

class GroupAttachmentResponseTest : ShouldSpec({

    should("correctly map GroupAttachmentResponse to GroupAttachment") {
        // given
        val groupAttachmentResponse = createGroupAttachmentResponse()

        // when
        val groupAttachment = groupAttachmentResponse.toDomain()

        // then
        groupAttachment.id shouldBe groupAttachmentResponse.id
    }
},)
