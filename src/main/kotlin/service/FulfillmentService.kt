package org.ecommerce.service

import org.ecommerce.dto.FulfillmentRequest
import org.ecommerce.dto.FulfillmentLineItemDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import kotlin.math.pow

@Service
class FulfillmentService(
    private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(FulfillmentService::class.java)

    fun fulfillOrder(orderId: String, lineItems: List<FulfillmentLineItemDto>, maxAttempts: Int = 3) {
        val requestBody = FulfillmentRequest(orderId, lineItems)

        repeat(maxAttempts) { attempt ->
            try {
                val response = webClient.post()
                    .uri("/wms/fulfill-order") // WMS API endpoint
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(WmsResponse::class.java)
                    .block(Duration.ofSeconds(5))

                if (response?.success == true) {
                    logger.info("Order $orderId fulfilled successfully at WMS")
                    return
                } else {
                    logger.warn("Attempt ${attempt + 1} failed: ${response?.message}")
                }
            } catch (ex: Exception) {
                logger.warn("Attempt ${attempt + 1} failed for order $orderId: ${ex.message}")
            }

            // Exponential backoff
            val backoff = 1000L * (2.0.pow(attempt.toDouble())).toLong()
            Thread.sleep(backoff)
        }

        logger.error("Order $orderId could not be fulfilled after $maxAttempts attempts. Needs manual intervention.")
        // Optional: send to a dead-letter topic or alerting system
    }

    data class WmsResponse(
        val success: Boolean,
        val message: String?
    )
}
