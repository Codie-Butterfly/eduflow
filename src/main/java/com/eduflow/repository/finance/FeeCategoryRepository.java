package com.eduflow.repository.finance;

import com.eduflow.entity.finance.FeeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeeCategoryRepository extends JpaRepository<FeeCategory, Long> {

    Optional<FeeCategory> findByName(FeeCategory.CategoryType name);

    boolean existsByName(FeeCategory.CategoryType name);

    List<FeeCategory> findByActive(boolean active);
}
