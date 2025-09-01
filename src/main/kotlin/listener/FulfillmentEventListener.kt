package org.ecommerce.listener

import org.ecommerce.event.OrderAllocatedEvent
import org.ecommerce.dto.FulfillmentLineItemDto
import org.ecommerce.service.FulfillmentService
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class FulfillmentEventListener(
    private val fulfillmentService: FulfillmentService
) {
    private val logger = LoggerFactory.getLogger(FulfillmentEventListener::class.java)

    @KafkaListener(topics = ["order-allocated"], groupId = "fulfillment-service")
    fun onOrderAllocated(event: OrderAllocatedEvent) {
        logger.info("Received OrderAllocatedEvent for order ${event.orderId}")

        val lineItemsDto = event.lineItems.map {
            FulfillmentLineItemDto(sku = it.sku, quantity = it.quantity)
        }

        fulfillmentService.fulfillOrder(event.orderId, lineItemsDto)
    }
}
