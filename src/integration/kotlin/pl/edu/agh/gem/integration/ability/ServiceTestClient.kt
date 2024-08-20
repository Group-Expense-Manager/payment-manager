package pl.edu.agh.gem.integration.ability

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.springframework.test.web.servlet.client.MockMvcWebTestClient.bindToApplicationContext
import org.springframework.web.context.WebApplicationContext
import pl.edu.agh.gem.headers.HeadersUtils.withAppAcceptType
import pl.edu.agh.gem.headers.HeadersUtils.withAppContentType
import pl.edu.agh.gem.headers.HeadersUtils.withValidatedUser
import pl.edu.agh.gem.paths.Paths.EXTERNAL
import pl.edu.agh.gem.security.GemUser
import java.net.URI

@Component
@Lazy
class ServiceTestClient(applicationContext: WebApplicationContext) {
    private val webClient = bindToApplicationContext(applicationContext)
        .configureClient()
        .build()

    fun createPayment(body: Any, user: GemUser, groupId: String): ResponseSpec {
        return webClient.post()
            .uri { it.path("$EXTERNAL/payments").queryParam("groupId", groupId).build() }
            .headers {
                it.withValidatedUser(user)
                it.withAppContentType()
            }
            .bodyValue(body)
            .exchange()
    }
    fun getPayment(user: GemUser, paymentId: String, groupId: String): ResponseSpec {
        return webClient.get()
            .uri(URI("$EXTERNAL/payments/$paymentId/groups/$groupId"))
            .headers { it.withValidatedUser(user).withAppAcceptType() }
            .exchange()
    }

    fun decide(body: Any, user: GemUser): ResponseSpec {
        return webClient.post()
            .uri(URI("$EXTERNAL/payments/decide"))
            .headers { it.withValidatedUser(user).withAppContentType() }
            .bodyValue(body)
            .exchange()
    }

    fun delete(user: GemUser, groupId: String, paymentId: String): ResponseSpec {
        return webClient.delete()
            .uri(URI("$EXTERNAL/payments/$paymentId/groups/$groupId"))
            .headers { it.withValidatedUser(user) }
            .exchange()
    }

    fun updatePayment(body: Any, user: GemUser, groupId: String, paymentId: String): ResponseSpec {
        return webClient.put()
            .uri(URI("$EXTERNAL/payments/$paymentId/groups/$groupId"))
            .headers {
                it.withValidatedUser(user)
                it.withAppContentType()
            }
            .bodyValue(body)
            .exchange()
    }
}
