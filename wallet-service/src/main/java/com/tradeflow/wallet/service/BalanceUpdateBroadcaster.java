package com.tradeflow.wallet.service;

import com.tradeflow.common.dto.WalletBalanceDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class BalanceUpdateBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public BalanceUpdateBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastBalanceUpdate(UUID userId, List<WalletBalanceDTO> balances) {
        List<WalletBalanceDTO> safeBalances = Objects.requireNonNull(balances, "balances must not be null");
        messagingTemplate.convertAndSend(
            "/topic/balances/" + userId,
            safeBalances
        );
    }
}
