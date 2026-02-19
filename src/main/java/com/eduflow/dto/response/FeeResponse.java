package com.eduflow.dto.response;

import com.eduflow.entity.finance.Fee;
import com.eduflow.entity.finance.FeeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeResponse {

    private Long id;
    private FeeCategory.CategoryType category;
    private String name;
    private BigDecimal amount;
    private String academicYear;
    private Fee.Term term;
    private String description;
    private boolean mandatory;
    private boolean active;
    private List<ClassSummary> applicableClasses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSummary {
        private Long id;
        private String name;
        private Integer grade;
    }
}
