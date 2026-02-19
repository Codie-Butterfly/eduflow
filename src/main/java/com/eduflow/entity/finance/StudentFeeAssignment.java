package com.eduflow.entity.finance;

import com.eduflow.entity.academic.Student;
import com.eduflow.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "student_fee_assignments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "fee_id", "academic_year"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFeeAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id", nullable = false)
    private Fee fee;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_reason")
    private String discountReason;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FeeStatus status = FeeStatus.PENDING;

    @OneToMany(mappedBy = "studentFeeAssignment", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Payment> payments = new HashSet<>();

    public enum FeeStatus {
        PENDING, PARTIAL, PAID, OVERDUE, WAIVED
    }

    public BigDecimal getBalance() {
        BigDecimal netAmount = amount.subtract(discountAmount);
        return netAmount.subtract(amountPaid);
    }

    public BigDecimal getNetAmount() {
        return amount.subtract(discountAmount);
    }

    public void addPayment(BigDecimal paymentAmount) {
        this.amountPaid = this.amountPaid.add(paymentAmount);
        updateStatus();
    }

    public void updateStatus() {
        BigDecimal netAmount = getNetAmount();
        if (amountPaid.compareTo(netAmount) >= 0) {
            this.status = FeeStatus.PAID;
        } else if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.status = FeeStatus.PARTIAL;
        } else if (LocalDate.now().isAfter(dueDate)) {
            this.status = FeeStatus.OVERDUE;
        } else {
            this.status = FeeStatus.PENDING;
        }
    }
}
