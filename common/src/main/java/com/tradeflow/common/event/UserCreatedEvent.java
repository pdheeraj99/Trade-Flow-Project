package com.tradeflow.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a new user is registered.
 * Used to trigger wallet creation in wallet-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID userId;
    private String username;
    private String email;
    private Instant createdAt;
}
