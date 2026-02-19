package com.eduflow.entity.finance;

import com.eduflow.entity.academic.SchoolClass;
import com.eduflow.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "fees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fee extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private FeeCategory category;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Enumerated(EnumType.STRING)
    private Term term;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean mandatory = true;

    @Builder.Default
    private boolean active = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "fee_applicable_classes",
            joinColumns = @JoinColumn(name = "fee_id"),
            inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    @Builder.Default
    private Set<SchoolClass> applicableClasses = new HashSet<>();

    @OneToMany(mappedBy = "fee", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<StudentFeeAssignment> assignments = new HashSet<>();

    public enum Term {
        TERM_1, TERM_2, TERM_3, ANNUAL
    }
}
