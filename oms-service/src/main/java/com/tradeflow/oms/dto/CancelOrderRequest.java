package com.tradeflow.oms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for cancelling an order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to cancel an existing order")
public class CancelOrderRequest {

    @Schema(description = "Order ID to cancel", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @Schema(description = "Reason for cancellation", example = "User requested cancellation")
    private String reason;
}
