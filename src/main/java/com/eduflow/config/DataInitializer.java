package com.eduflow.config;

import com.eduflow.entity.finance.FeeCategory;
import com.eduflow.entity.user.Permission;
import com.eduflow.entity.user.Role;
import com.eduflow.entity.user.User;
import com.eduflow.repository.finance.FeeCategoryRepository;
import com.eduflow.repository.user.PermissionRepository;
import com.eduflow.repository.user.RoleRepository;
import com.eduflow.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final FeeCategoryRepository feeCategoryRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        initializePermissions();
        initializeRoles();
        initializeFeeCategories();
        initializeDefaultAdmin();
        resetSequences();
        log.info("Data initialization completed");
    }

    private void resetSequences() {
        log.info("Starting database sequence synchronization...");

        List<String> tables = Arrays.asList(
                "users", "teachers", "students", "parents", "school_classes",
                "subjects", "enrollments", "grades", "fees", "payments",
                "student_fee_assignments", "announcements", "homework",
                "notifications", "student_reports", "roles", "permissions",
                "fee_categories", "payment_plans", "payment_plan_installments",
                "payment_transactions"
        );

        int successCount = 0;
        int skipCount = 0;

        for (String table : tables) {
            try {
                // First get the current max ID
                String maxIdSql = String.format("SELECT COALESCE(MAX(id), 0) FROM %s", table);
                Number maxId = (Number) entityManager.createNativeQuery(maxIdSql).getSingleResult();

                // Then reset the sequence
                String sequenceName = table + "_id_seq";
                long newSequenceValue = maxId.longValue() + 1;
                String resetSql = String.format(
                        "SELECT setval('%s', %d, false)",
                        sequenceName, newSequenceValue
                );
                Number result = (Number) entityManager.createNativeQuery(resetSql).getSingleResult();

                log.info("Sequence reset: {} -> {} (max_id={}, next_val={})",
                        sequenceName, result, maxId, newSequenceValue);
                successCount++;
            } catch (Exception e) {
                log.warn("Skipped sequence for table '{}': {}", table, e.getMessage());
                skipCount++;
            }
        }

        log.info("Database sequence synchronization completed: {} successful, {} skipped",
                successCount, skipCount);
    }

    private void initializePermissions() {
        List<String> permissions = Arrays.asList(
                "USER_READ", "USER_WRITE", "USER_DELETE",
                "STUDENT_READ", "STUDENT_WRITE", "STUDENT_DELETE",
                "TEACHER_READ", "TEACHER_WRITE", "TEACHER_DELETE",
                "CLASS_READ", "CLASS_WRITE", "CLASS_DELETE",
                "SUBJECT_READ", "SUBJECT_WRITE", "SUBJECT_DELETE",
                "FEE_READ", "FEE_WRITE", "FEE_DELETE",
                "PAYMENT_READ", "PAYMENT_WRITE",
                "GRADE_READ", "GRADE_WRITE",
                "REPORT_READ", "REPORT_WRITE",
                "ANNOUNCEMENT_READ", "ANNOUNCEMENT_WRITE"
        );

        for (String permissionName : permissions) {
            if (!permissionRepository.existsByName(permissionName)) {
                Permission permission = Permission.builder()
                        .name(permissionName)
                        .description(permissionName.replace("_", " ").toLowerCase())
                        .build();
                permissionRepository.save(permission);
            }
        }
        log.info("Permissions initialized");
    }

    private void initializeRoles() {
        // Admin role - all permissions
        if (!roleRepository.existsByName(Role.RoleName.ADMIN)) {
            Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());
            Role adminRole = Role.builder()
                    .name(Role.RoleName.ADMIN)
                    .description("System Administrator")
                    .permissions(allPermissions)
                    .build();
            roleRepository.save(adminRole);
        }

        // Teacher role
        if (!roleRepository.existsByName(Role.RoleName.TEACHER)) {
            Set<Permission> teacherPermissions = new HashSet<>();
            Arrays.asList("STUDENT_READ", "CLASS_READ", "SUBJECT_READ", "GRADE_READ", "GRADE_WRITE",
                    "REPORT_READ", "REPORT_WRITE", "ANNOUNCEMENT_READ").forEach(p ->
                    permissionRepository.findByName(p).ifPresent(teacherPermissions::add)
            );
            Role teacherRole = Role.builder()
                    .name(Role.RoleName.TEACHER)
                    .description("Teacher")
                    .permissions(teacherPermissions)
                    .build();
            roleRepository.save(teacherRole);
        }

        // Parent role
        if (!roleRepository.existsByName(Role.RoleName.PARENT)) {
            Set<Permission> parentPermissions = new HashSet<>();
            Arrays.asList("STUDENT_READ", "FEE_READ", "PAYMENT_READ", "PAYMENT_WRITE",
                    "GRADE_READ", "REPORT_READ", "ANNOUNCEMENT_READ").forEach(p ->
                    permissionRepository.findByName(p).ifPresent(parentPermissions::add)
            );
            Role parentRole = Role.builder()
                    .name(Role.RoleName.PARENT)
                    .description("Parent/Guardian")
                    .permissions(parentPermissions)
                    .build();
            roleRepository.save(parentRole);
        }

        // Student role
        if (!roleRepository.existsByName(Role.RoleName.STUDENT)) {
            Set<Permission> studentPermissions = new HashSet<>();
            Arrays.asList("GRADE_READ", "REPORT_READ", "ANNOUNCEMENT_READ").forEach(p ->
                    permissionRepository.findByName(p).ifPresent(studentPermissions::add)
            );
            Role studentRole = Role.builder()
                    .name(Role.RoleName.STUDENT)
                    .description("Student")
                    .permissions(studentPermissions)
                    .build();
            roleRepository.save(studentRole);
        }

        log.info("Roles initialized");
    }

    private void initializeFeeCategories() {
        for (FeeCategory.CategoryType categoryType : FeeCategory.CategoryType.values()) {
            if (!feeCategoryRepository.existsByName(categoryType)) {
                FeeCategory category = FeeCategory.builder()
                        .name(categoryType)
                        .description(categoryType.name().replace("_", " ") + " fees")
                        .active(true)
                        .build();
                feeCategoryRepository.save(category);
            }
        }
        log.info("Fee categories initialized");
    }

    private void initializeDefaultAdmin() {
        String adminEmail = "admin@eduflow.com";
        if (!userRepository.existsByEmail(adminEmail)) {
            Role adminRole = roleRepository.findByName(Role.RoleName.ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));

            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("System")
                    .lastName("Administrator")
                    .enabled(true)
                    .build();
            admin.addRole(adminRole);
            userRepository.save(admin);

            log.info("Default admin user created: {}", adminEmail);
        }
    }
}
