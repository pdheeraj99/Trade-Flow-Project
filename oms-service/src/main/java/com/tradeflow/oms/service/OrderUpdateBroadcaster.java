package com.tradeflow.oms.service;

import com.tradeflow.oms.event.OrderStatusUpdateEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderUpdateBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public OrderUpdateBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastOrderUpdate(OrderStatusUpdateEvent event) {
        messagingTemplate.convertAndSend(
            "/topic/orders/" + event.getUserId(),
            event
        );
    }
}
