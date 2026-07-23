package com.swp5.library_management.service;

import com.swp5.library_management.entity.FineInvoice;

/**
 * Service specifically for fine-related email notifications.
 * Created to keep fine logic separate from other services.
 */
public interface FineEmailService {
    void sendFinePaymentConfirmation(FineInvoice fineInvoice);
}
