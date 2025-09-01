package org.ecommerce.service

import org.ecommerce.dto.FulfillmentLineItemDto
import org.ecommerce.dto.FulfillmentRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec
import reactor.core.publisher.Mono

class FulfillmentServiceTest {

    @Mock
    lateinit var webClient: WebClient

    @Mock
    lateinit var uriSpec: RequestBodyUriSpec

    @Mock
    lateinit var bodySpec: RequestBodySpec

    @Mock
    lateinit var responseSpec: ResponseSpec

    lateinit var fulfillmentService: FulfillmentService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        fulfillmentService = FulfillmentService(webClient)
    }

    @Test
    fun `fulfillOrder succeeds on first attempt`() {
        val orderId = "123"
        val lineItems = listOf(FulfillmentLineItemDto("item1", 2))

        // WebClient mock chain
        `when`(webClient.post()).thenReturn(uriSpec)
        `when`(uriSpec.uri("/wms/fulfill-order")).thenReturn(bodySpec)
        `when`(bodySpec.bodyValue(any(FulfillmentRequest::class.java))).thenReturn(bodySpec)
        `when`(bodySpec.retrieve()).thenReturn(responseSpec)
        `when`(responseSpec.bodyToMono(FulfillmentService.WmsResponse::class.java))
            .thenReturn(Mono.just(FulfillmentService.WmsResponse(true, "Success")))

        fulfillmentService.fulfillOrder(orderId, lineItems)

        // Verify WebClient called exactly once
        verify(webClient, times(1)).post()
    }

    @Test
    fun `fulfillOrder retries on failure and eventually succeeds`() {
        val orderId = "456"
        val lineItems = listOf(FulfillmentLineItemDto("item2", 1))

        `when`(webClient.post()).thenReturn(uriSpec)
        `when`(uriSpec.uri("/wms/fulfill-order")).thenReturn(bodySpec)
        `when`(bodySpec.bodyValue(any(FulfillmentRequest::class.java))).thenReturn(bodySpec)
        `when`(bodySpec.retrieve()).thenReturn(responseSpec)

        // First two attempts fail, third succeeds
        `when`(responseSpec.bodyToMono(FulfillmentService.WmsResponse::class.java))
            .thenReturn(
                Mono.just(FulfillmentService.WmsResponse(false, "Fail 1")),
                Mono.just(FulfillmentService.WmsResponse(false, "Fail 2")),
                Mono.just(FulfillmentService.WmsResponse(true, "Success"))
            )

        fulfillmentService.fulfillOrder(orderId, lineItems, maxAttempts = 3)

        // Verify WebClient called three times due to retries
        verify(webClient, times(3)).post()
    }

    @Test
    fun `fulfillOrder retries maxAttempts and fails`() {
        val orderId = "789"
        val lineItems = listOf(FulfillmentLineItemDto("item3", 5))

        `when`(webClient.post()).thenReturn(uriSpec)
        `when`(uriSpec.uri("/wms/fulfill-order")).thenReturn(bodySpec)
        `when`(bodySpec.bodyValue(any(FulfillmentRequest::class.java))).thenReturn(bodySpec)
        `when`(bodySpec.retrieve()).thenReturn(responseSpec)
        `when`(responseSpec.bodyToMono(FulfillmentService.WmsResponse::class.java))
            .thenReturn(Mono.just(FulfillmentService.WmsResponse(false, "Fail")))

        fulfillmentService.fulfillOrder(orderId, lineItems, maxAttempts = 3)

        // Verify WebClient called maxAttempts times
        verify(webClient, times(3)).post()
    }
}
