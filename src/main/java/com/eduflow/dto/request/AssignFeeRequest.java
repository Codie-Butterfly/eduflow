package com.eduflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignFeeRequest {

    @NotNull(message = "Fee ID is required")
    private Long feeId;

    // Either studentIds or classIds should be provided
    private Set<Long> studentIds;
    private Set<Long> classIds;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    private BigDecimal discountAmount;
    private String discountReason;
}
