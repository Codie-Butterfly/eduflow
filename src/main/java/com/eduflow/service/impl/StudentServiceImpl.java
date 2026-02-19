package com.eduflow.service.impl;

import com.eduflow.dto.request.CreateStudentRequest;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.StudentResponse;
import com.eduflow.entity.academic.Enrollment;
import com.eduflow.entity.academic.Parent;
import com.eduflow.entity.academic.SchoolClass;
import com.eduflow.entity.academic.Student;
import com.eduflow.entity.user.Role;
import com.eduflow.entity.user.User;
import com.eduflow.exception.BadRequestException;
import com.eduflow.exception.DuplicateResourceException;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.EnrollmentRepository;
import com.eduflow.repository.academic.ParentRepository;
import com.eduflow.repository.academic.SchoolClassRepository;
import com.eduflow.repository.academic.StudentRepository;
import com.eduflow.repository.user.RoleRepository;
import com.eduflow.repository.user.UserRepository;
import com.eduflow.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ParentRepository parentRepository;
    private final SchoolClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StudentResponse createStudent(CreateStudentRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role studentRole = roleRepository.findByName(Role.RoleName.STUDENT)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "STUDENT"));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(generateTemporaryPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .enabled(true)
                .build();
        user.addRole(studentRole);
        user = userRepository.save(user);

        String studentId = generateStudentId();

        Student student = Student.builder()
                .studentId(studentId)
                .user(user)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .enrollmentDate(LocalDate.now())
                .address(request.getAddress())
                .bloodGroup(request.getBloodGroup())
                .medicalConditions(request.getMedicalConditions())
                .status(Student.StudentStatus.ACTIVE)
                .build();

        if (request.getParentId() != null) {
            Parent parent = parentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent", "id", request.getParentId()));
            student.setParent(parent);
        }

        if (request.getClassId() != null) {
            SchoolClass schoolClass = classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class", "id", request.getClassId()));
            student.setCurrentClass(schoolClass);
        }

        student = studentRepository.save(student);
        log.info("Student created successfully: {}", student.getStudentId());

        return mapToResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
        return mapToResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getStudentByStudentId(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "studentId", studentId));
        return mapToResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StudentResponse> getAllStudents(Pageable pageable) {
        Page<Student> page = studentRepository.findAll(pageable);
        List<StudentResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getStudentsByClassId(Long classId) {
        return studentRepository.findByCurrentClassId(classId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getStudentsByParentId(Long parentId) {
        return studentRepository.findByParentId(parentId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public StudentResponse updateStudent(Long id, CreateStudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        User user = student.getUser();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        userRepository.save(user);

        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setAddress(request.getAddress());
        student.setBloodGroup(request.getBloodGroup());
        student.setMedicalConditions(request.getMedicalConditions());

        if (request.getParentId() != null) {
            Parent parent = parentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent", "id", request.getParentId()));
            student.setParent(parent);
        }

        student = studentRepository.save(student);
        log.info("Student updated: {}", student.getStudentId());

        return mapToResponse(student);
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        student.setStatus(Student.StudentStatus.INACTIVE);
        student.getUser().setEnabled(false);
        studentRepository.save(student);
        log.info("Student deactivated: {}", student.getStudentId());
    }

    @Override
    @Transactional
    public StudentResponse enrollStudentInClass(Long studentId, Long classId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        // Validate academicYear is set on the class
        if (schoolClass.getAcademicYear() == null || schoolClass.getAcademicYear().isBlank()) {
            throw new BadRequestException("Cannot enroll student: Class does not have an academic year set");
        }

        // Check if student is already enrolled in this class for this academic year
        if (enrollmentRepository.existsByStudentIdAndSchoolClassIdAndAcademicYear(
                studentId, classId, schoolClass.getAcademicYear())) {
            throw new BadRequestException("Student is already enrolled in this class for academic year "
                    + schoolClass.getAcademicYear());
        }

        if (schoolClass.getMaxCapacity() != null) {
            Long currentCount = classRepository.countStudentsByClassId(classId);
            if (currentCount >= schoolClass.getMaxCapacity()) {
                throw new BadRequestException("Class has reached maximum capacity");
            }
        }

        student.setCurrentClass(schoolClass);

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .schoolClass(schoolClass)
                .academicYear(schoolClass.getAcademicYear())
                .enrollmentDate(LocalDate.now())
                .status(Enrollment.EnrollmentStatus.ACTIVE)
                .build();

        enrollmentRepository.save(enrollment);
        student = studentRepository.save(student);

        log.info("Student {} enrolled in class {}", student.getStudentId(), schoolClass.getName());
        return mapToResponse(student);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StudentResponse> searchStudents(String name, Pageable pageable) {
        Page<Student> page = studentRepository.searchByName(name, pageable);
        List<StudentResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<StudentResponse> getStudentsByStatus(Student.StudentStatus status, Pageable pageable) {
        Page<Student> page = studentRepository.findByStatus(status, pageable);
        List<StudentResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private String generateStudentId() {
        String year = String.valueOf(LocalDate.now().getYear());
        String random = String.format("%04d", (int) (Math.random() * 10000));
        return "STU" + year + random;
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private StudentResponse mapToResponse(Student student) {
        StudentResponse.ClassSummary classSummary = null;
        if (student.getCurrentClass() != null) {
            SchoolClass sc = student.getCurrentClass();
            classSummary = StudentResponse.ClassSummary.builder()
                    .id(sc.getId())
                    .name(sc.getName())
                    .grade(sc.getGrade())
                    .academicYear(sc.getAcademicYear())
                    .build();
        }

        StudentResponse.ParentSummary parentSummary = null;
        if (student.getParent() != null) {
            Parent p = student.getParent();
            parentSummary = StudentResponse.ParentSummary.builder()
                    .id(p.getId())
                    .name(p.getUser().getFullName())
                    .phone(p.getUser().getPhone())
                    .email(p.getUser().getEmail())
                    .build();
        }

        User user = student.getUser();
        return StudentResponse.builder()
                .id(student.getId())
                .studentId(student.getStudentId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .enrollmentDate(student.getEnrollmentDate())
                .address(student.getAddress())
                .bloodGroup(student.getBloodGroup())
                .status(student.getStatus())
                .currentClass(classSummary)
                .parent(parentSummary)
                .build();
    }
}
