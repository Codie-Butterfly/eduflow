package com.eduflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalStudents;
    private long totalTeachers;
    private long totalClasses;
    private BigDecimal feesCollected;
    private BigDecimal outstandingFees;
    private long totalPayments;
    private long pendingPayments;
}
