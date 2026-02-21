package com.eduflow.service.impl;

import com.eduflow.dto.request.CreateTeacherRequest;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.TeacherResponse;
import com.eduflow.entity.academic.SchoolClass;
import com.eduflow.entity.academic.Subject;
import com.eduflow.entity.academic.Teacher;
import com.eduflow.entity.user.Role;
import com.eduflow.entity.user.User;
import com.eduflow.exception.DuplicateResourceException;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.SubjectRepository;
import com.eduflow.repository.academic.TeacherRepository;
import com.eduflow.repository.user.RoleRepository;
import com.eduflow.repository.user.UserRepository;
import com.eduflow.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SubjectRepository subjectRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public TeacherResponse createTeacher(CreateTeacherRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        Role teacherRole = roleRepository.findByName(Role.RoleName.TEACHER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "TEACHER"));

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(generateTemporaryPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .enabled(true)
                .build();
        user.addRole(teacherRole);
        user = userRepository.save(user);

        String employeeId = generateEmployeeId();

        Teacher teacher = Teacher.builder()
                .employeeId(employeeId)
                .user(user)
                .qualification(request.getQualification())
                .specialization(request.getSpecialization())
                .dateOfJoining(request.getDateOfJoining() != null ? request.getDateOfJoining() : LocalDate.now())
                .address(request.getAddress())
                .build();

        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(request.getSubjectIds()));
            teacher.setSubjects(subjects);
        }

        teacher = teacherRepository.save(teacher);
        log.info("Teacher created: {}", teacher.getEmployeeId());

        return mapToResponse(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getTeacherById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));
        return mapToResponse(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getTeacherByEmployeeId(String employeeId) {
        Teacher teacher = teacherRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "employeeId", employeeId));
        return mapToResponse(teacher);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TeacherResponse> getAllTeachers(Pageable pageable) {
        Page<Teacher> page = teacherRepository.findAll(pageable);
        List<TeacherResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherResponse> searchTeachers(String name) {
        return teacherRepository.searchByName(name).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherResponse updateTeacher(Long id, CreateTeacherRequest request) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));

        User user = teacher.getUser();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        userRepository.save(user);

        teacher.setQualification(request.getQualification());
        teacher.setSpecialization(request.getSpecialization());
        teacher.setAddress(request.getAddress());
        if (request.getDateOfJoining() != null) {
            teacher.setDateOfJoining(request.getDateOfJoining());
        }

        if (request.getSubjectIds() != null) {
            Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(request.getSubjectIds()));
            teacher.setSubjects(subjects);
        }

        teacher = teacherRepository.save(teacher);
        log.info("Teacher updated: {}", teacher.getId());

        return mapToResponse(teacher);
    }

    @Override
    @Transactional
    public void deleteTeacher(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", id));

        teacher.getUser().setEnabled(false);
        userRepository.save(teacher.getUser());
        log.info("Teacher deactivated: {}", id);
    }

    @Override
    @Transactional
    public TeacherResponse addSubjectToTeacher(Long teacherId, Long subjectId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));

        teacher.addSubject(subject);
        teacher = teacherRepository.save(teacher);
        log.info("Subject {} added to teacher {}", subjectId, teacherId);

        return mapToResponse(teacher);
    }

    @Override
    @Transactional
    public TeacherResponse removeSubjectFromTeacher(Long teacherId, Long subjectId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));

        teacher.removeSubject(subject);
        teacher = teacherRepository.save(teacher);
        log.info("Subject {} removed from teacher {}", subjectId, teacherId);

        return mapToResponse(teacher);
    }

    private String generateEmployeeId() {
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = "TCH" + year;
        Integer maxNum = teacherRepository.findMaxEmployeeIdNumber(prefix);
        int nextNum = (maxNum != null ? maxNum : 0) + 1;
        return prefix + String.format("%04d", nextNum);
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private TeacherResponse mapToResponse(Teacher teacher) {
        List<TeacherResponse.SubjectSummary> subjectSummaries = teacher.getSubjects().stream()
                .map(s -> TeacherResponse.SubjectSummary.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .code(s.getCode())
                        .build())
                .collect(Collectors.toList());

        List<TeacherResponse.ClassSummary> classSummaries = teacher.getAssignedClasses().stream()
                .map(c -> TeacherResponse.ClassSummary.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .grade(c.getGrade())
                        .academicYear(c.getAcademicYear())
                        .build())
                .collect(Collectors.toList());

        User user = teacher.getUser();
        return TeacherResponse.builder()
                .id(teacher.getId())
                .employeeId(teacher.getEmployeeId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .qualification(teacher.getQualification())
                .specialization(teacher.getSpecialization())
                .dateOfJoining(teacher.getDateOfJoining())
                .address(teacher.getAddress())
                .subjects(subjectSummaries)
                .assignedClasses(classSummaries)
                .build();
    }
}
