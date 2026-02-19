package com.eduflow.entity.finance;

import com.eduflow.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "fee_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeCategory extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private CategoryType name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean active = true;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Fee> fees = new HashSet<>();

    public enum CategoryType {
        TUITION,
        TRANSPORT,
        BOARDING,
        EXAM,
        ACTIVITY,
        LIBRARY,
        LABORATORY,
        UNIFORM,
        BOOKS,
        OTHER
    }
}
