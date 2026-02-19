package com.eduflow.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookRequest {

    private String eventType;
    private String gatewayReference;
    private String merchantReference;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String customerPhone;
    private String customerName;
    private Long timestamp;
    private String transactionId;
    private String failureReason;
    private Object metadata;
}
