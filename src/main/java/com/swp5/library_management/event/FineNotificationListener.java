package com.swp5.library_management.event;

import com.swp5.library_management.service.FineEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FineNotificationListener {

    private final FineEmailService fineEmailService;

    @Async
    @EventListener
    public void handleFinePaidEvent(FinePaidEvent event) {
        fineEmailService.sendFinePaymentConfirmation(event.getFineInvoice());
    }
}
