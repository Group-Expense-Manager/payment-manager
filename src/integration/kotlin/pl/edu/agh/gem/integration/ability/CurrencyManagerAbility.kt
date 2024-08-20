package pl.edu.agh.gem.integration.ability

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatusCode
import pl.edu.agh.gem.headers.HeadersTestUtils.withAppContentType
import pl.edu.agh.gem.integration.environment.ProjectConfig.wiremock
import pl.edu.agh.gem.paths.Paths.INTERNAL

private fun createGroupDataUrl() =
    "$INTERNAL/currencies"

private fun createExchangeRateUrl(baseCurrency: String, targetCurrency: String) = "$INTERNAL/currencies/from/$baseCurrency/to/$targetCurrency/.*"

fun stubCurrencyManagerAvailableCurrencies(body: Any?, statusCode: HttpStatusCode = OK) {
    wiremock.stubFor(
        get(urlMatching(createGroupDataUrl()))
            .willReturn(
                aResponse()
                    .withStatus(statusCode.value())
                    .withAppContentType()
                    .withBody(
                        jacksonObjectMapper().writeValueAsString(body),
                    ),
            ),
    )
}

fun stubCurrencyManagerExchangeRate(body: Any?, baseCurrency: String, targetCurrency: String, statusCode: HttpStatusCode = OK) {
    wiremock.stubFor(
        get(urlPathMatching(createExchangeRateUrl(baseCurrency, targetCurrency)))
            .willReturn(
                aResponse()
                    .withStatus(statusCode.value())
                    .withAppContentType()
                    .withBody(
                        jacksonObjectMapper().registerModules(JavaTimeModule()).writeValueAsString(body),
                    ),
            ),
    )
}
