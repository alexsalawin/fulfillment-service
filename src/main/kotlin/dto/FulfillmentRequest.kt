package org.ecommerce.dto

data class FulfillmentRequest(
    val orderId: String,
    val lineItems: List<FulfillmentLineItemDto>
)

data class FulfillmentLineItemDto(
    val sku: String,
    val quantity: Int
)
