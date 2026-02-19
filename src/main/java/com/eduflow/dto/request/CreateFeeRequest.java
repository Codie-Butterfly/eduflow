package com.eduflow.dto.request;

import com.eduflow.entity.finance.Fee;
import com.eduflow.entity.finance.FeeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeeRequest {

    @NotNull(message = "Fee category is required")
    private FeeCategory.CategoryType category;

    @NotBlank(message = "Fee name is required")
    private String name;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    private Fee.Term term;

    private String description;

    private boolean mandatory = true;

    private Set<Long> applicableClassIds;
}
