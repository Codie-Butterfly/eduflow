package com.eduflow.service.payment;

import com.eduflow.entity.finance.Payment;

public interface PaymentGatewayService {

    String initiatePayment(Payment payment);

    boolean verifyPayment(String gatewayRef);

    void processRefund(String gatewayRef, java.math.BigDecimal amount);
}
