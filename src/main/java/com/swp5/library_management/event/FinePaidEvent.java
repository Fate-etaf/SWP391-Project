package com.swp5.library_management.event;

import com.swp5.library_management.entity.FineInvoice;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when a fine is paid.
 */
@Getter
public class FinePaidEvent extends ApplicationEvent {
    private final FineInvoice fineInvoice;

    public FinePaidEvent(Object source, FineInvoice fineInvoice) {
        super(source);
        this.fineInvoice = fineInvoice;
    }
}
