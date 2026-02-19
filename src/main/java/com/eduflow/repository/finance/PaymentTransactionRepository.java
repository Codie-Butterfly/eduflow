package com.eduflow.repository.finance;

import com.eduflow.entity.finance.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByGatewayRef(String gatewayRef);

    List<PaymentTransaction> findByPaymentId(Long paymentId);

    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    boolean existsByGatewayRef(String gatewayRef);
}
