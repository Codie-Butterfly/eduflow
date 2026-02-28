package com.eduflow.service.impl;

import com.eduflow.dto.request.AssignFeeRequest;
import com.eduflow.dto.request.CreateFeeRequest;
import com.eduflow.dto.response.FeeResponse;
import com.eduflow.dto.response.StudentFeeResponse;
import com.eduflow.entity.academic.SchoolClass;
import com.eduflow.entity.academic.Student;
import com.eduflow.entity.finance.Fee;
import com.eduflow.entity.finance.FeeCategory;
import com.eduflow.entity.finance.Payment;
import com.eduflow.entity.finance.StudentFeeAssignment;
import com.eduflow.exception.BadRequestException;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.SchoolClassRepository;
import com.eduflow.repository.academic.StudentRepository;
import com.eduflow.repository.finance.FeeCategoryRepository;
import com.eduflow.repository.finance.FeeRepository;
import com.eduflow.repository.finance.StudentFeeAssignmentRepository;
import com.eduflow.service.FeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeServiceImpl implements FeeService {

    private final FeeRepository feeRepository;
    private final FeeCategoryRepository categoryRepository;
    private final StudentFeeAssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository classRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StudentFeeResponse> getAllFeeAssignments() {
        return assignmentRepository.findAll().stream()
                .map(this::mapToStudentFeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FeeResponse createFee(CreateFeeRequest request) {
        FeeCategory category = categoryRepository.findByName(request.getCategory())
                .orElseGet(() -> {
                    FeeCategory newCategory = FeeCategory.builder()
                            .name(request.getCategory())
                            .active(true)
                            .build();
                    return categoryRepository.save(newCategory);
                });

        Fee fee = Fee.builder()
                .category(category)
                .name(request.getName())
                .amount(request.getAmount())
                .academicYear(request.getAcademicYear())
                .term(request.getTerm())
                .description(request.getDescription())
                .mandatory(request.isMandatory())
                .active(true)
                .build();

        if (request.getApplicableClassIds() != null && !request.getApplicableClassIds().isEmpty()) {
            Set<SchoolClass> classes = new HashSet<>();
            for (Long classId : request.getApplicableClassIds()) {
                SchoolClass schoolClass = classRepository.findById(classId)
                        .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
                classes.add(schoolClass);
            }
            fee.setApplicableClasses(classes);
        }

        fee = feeRepository.save(fee);
        log.info("Fee created: {} - {}", fee.getName(), fee.getAmount());

        return mapToFeeResponse(fee);
    }

    @Override
    @Transactional(readOnly = true)
    public FeeResponse getFeeById(Long id) {
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee", "id", id));
        return mapToFeeResponse(fee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeResponse> getAllFees() {
        return feeRepository.findAll().stream()
                .map(this::mapToFeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeeResponse> getFeesByAcademicYear(String academicYear) {
        return feeRepository.findByAcademicYear(academicYear).stream()
                .map(this::mapToFeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FeeResponse updateFee(Long id, CreateFeeRequest request) {
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee", "id", id));

        fee.setName(request.getName());
        fee.setAmount(request.getAmount());
        fee.setDescription(request.getDescription());
        fee.setMandatory(request.isMandatory());

        if (request.getApplicableClassIds() != null) {
            fee.getApplicableClasses().clear();
            for (Long classId : request.getApplicableClassIds()) {
                SchoolClass schoolClass = classRepository.findById(classId)
                        .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
                fee.getApplicableClasses().add(schoolClass);
            }
        }

        fee = feeRepository.save(fee);
        log.info("Fee updated: {}", fee.getName());

        return mapToFeeResponse(fee);
    }

    @Override
    @Transactional
    public void deleteFee(Long id) {
        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fee", "id", id));

        fee.setActive(false);
        feeRepository.save(fee);
        log.info("Fee deactivated: {}", fee.getName());
    }

    @Override
    @Transactional
    public List<StudentFeeResponse> assignFeesToStudents(AssignFeeRequest request) {
        Fee fee = feeRepository.findById(request.getFeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Fee", "id", request.getFeeId()));

        Set<Student> students = new HashSet<>();

        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            for (Long studentId : request.getStudentIds()) {
                Student student = studentRepository.findById(studentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));
                students.add(student);
            }
        }

        if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
            for (Long classId : request.getClassIds()) {
                List<Student> classStudents = studentRepository.findByCurrentClassId(classId);
                students.addAll(classStudents);
            }
        }

        if (students.isEmpty()) {
            throw new BadRequestException("No students specified for fee assignment");
        }

        List<StudentFeeAssignment> assignments = new ArrayList<>();
        BigDecimal discountAmount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;

        for (Student student : students) {
            if (assignmentRepository.findByStudentIdAndFeeIdAndAcademicYear(
                    student.getId(), fee.getId(), fee.getAcademicYear()).isEmpty()) {

                StudentFeeAssignment assignment = StudentFeeAssignment.builder()
                        .student(student)
                        .fee(fee)
                        .academicYear(fee.getAcademicYear())
                        .dueDate(request.getDueDate())
                        .amount(fee.getAmount())
                        .discountAmount(discountAmount)
                        .discountReason(request.getDiscountReason())
                        .amountPaid(BigDecimal.ZERO)
                        .status(StudentFeeAssignment.FeeStatus.PENDING)
                        .build();

                assignments.add(assignmentRepository.save(assignment));
            }
        }

        log.info("Fee {} assigned to {} students", fee.getName(), assignments.size());

        return assignments.stream()
                .map(this::mapToStudentFeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentFeeResponse> getStudentFees(Long studentId) {
        return assignmentRepository.findByStudentId(studentId).stream()
                .map(this::mapToStudentFeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentFeeResponse> getStudentFeesByYear(Long studentId, String academicYear) {
        return assignmentRepository.findByStudentIdAndAcademicYear(studentId, academicYear).stream()
                .map(this::mapToStudentFeeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentFeeResponse applyDiscount(Long assignmentId, BigDecimal discountAmount, String reason) {
        StudentFeeAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee Assignment", "id", assignmentId));

        if (discountAmount.compareTo(assignment.getAmount()) > 0) {
            throw new BadRequestException("Discount cannot exceed fee amount");
        }

        assignment.setDiscountAmount(discountAmount);
        assignment.setDiscountReason(reason);
        assignment.updateStatus();

        assignment = assignmentRepository.save(assignment);
        log.info("Discount applied to fee assignment: {} - {}", assignmentId, discountAmount);

        return mapToStudentFeeResponse(assignment);
    }

    @Override
    @Transactional
    public StudentFeeResponse waiveFee(Long assignmentId, String reason) {
        StudentFeeAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee Assignment", "id", assignmentId));

        assignment.setStatus(StudentFeeAssignment.FeeStatus.WAIVED);
        assignment.setDiscountAmount(assignment.getAmount());
        assignment.setDiscountReason(reason);

        assignment = assignmentRepository.save(assignment);
        log.info("Fee waived: {} - {}", assignmentId, reason);

        return mapToStudentFeeResponse(assignment);
    }

    private FeeResponse mapToFeeResponse(Fee fee) {
        List<FeeResponse.ClassSummary> classes = fee.getApplicableClasses().stream()
                .map(c -> FeeResponse.ClassSummary.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .grade(c.getGrade())
                        .build())
                .collect(Collectors.toList());

        return FeeResponse.builder()
                .id(fee.getId())
                .category(fee.getCategory().getName())
                .name(fee.getName())
                .amount(fee.getAmount())
                .academicYear(fee.getAcademicYear())
                .term(fee.getTerm())
                .description(fee.getDescription())
                .mandatory(fee.isMandatory())
                .active(fee.isActive())
                .applicableClasses(classes)
                .build();
    }

    private StudentFeeResponse mapToStudentFeeResponse(StudentFeeAssignment assignment) {
        List<StudentFeeResponse.PaymentSummary> payments = List.of();
        if (assignment.getPayments() != null) {
            payments = assignment.getPayments().stream()
                    .map(p -> StudentFeeResponse.PaymentSummary.builder()
                            .id(p.getId())
                            .amount(p.getAmount())
                            .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null)
                            .transactionRef(p.getTransactionRef())
                            .status(p.getStatus() != null ? p.getStatus().name() : null)
                            .paidAt(p.getPaidAt() != null ? p.getPaidAt().toString() : null)
                            .build())
                    .collect(Collectors.toList());
        }

        StudentFeeResponse.StudentSummary studentSummary = null;
        if (assignment.getStudent() != null) {
            var student = assignment.getStudent();
            var user = student.getUser();
            studentSummary = StudentFeeResponse.StudentSummary.builder()
                    .id(student.getId())
                    .studentId(student.getStudentId())
                    .firstName(user != null ? user.getFirstName() : null)
                    .lastName(user != null ? user.getLastName() : null)
                    .fullName(user != null ? user.getFullName() : null)
                    .email(user != null ? user.getEmail() : null)
                    .className(student.getCurrentClass() != null ? student.getCurrentClass().getName() : null)
                    .build();
        }

        Fee fee = assignment.getFee();
        return StudentFeeResponse.builder()
                .id(assignment.getId())
                .feeName(fee.getName())
                .category(fee.getCategory().getName())
                .academicYear(assignment.getAcademicYear())
                .dueDate(assignment.getDueDate())
                .amount(assignment.getAmount())
                .discountAmount(assignment.getDiscountAmount())
                .discountReason(assignment.getDiscountReason())
                .netAmount(assignment.getNetAmount())
                .amountPaid(assignment.getAmountPaid())
                .balance(assignment.getBalance())
                .status(assignment.getStatus())
                .student(studentSummary)
                .payments(payments)
                .build();
    }
}
