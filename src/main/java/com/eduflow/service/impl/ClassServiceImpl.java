package com.eduflow.service.impl;

import com.eduflow.dto.request.CreateClassRequest;
import com.eduflow.dto.response.ClassResponse;
import com.eduflow.dto.response.PagedResponse;
import com.eduflow.dto.response.StudentResponse;
import com.eduflow.entity.academic.SchoolClass;
import com.eduflow.entity.academic.Student;
import com.eduflow.entity.academic.Subject;
import com.eduflow.entity.academic.Teacher;
import com.eduflow.exception.BadRequestException;
import com.eduflow.exception.DuplicateResourceException;
import com.eduflow.exception.ResourceNotFoundException;
import com.eduflow.repository.academic.SchoolClassRepository;
import com.eduflow.repository.academic.StudentRepository;
import com.eduflow.repository.academic.SubjectRepository;
import com.eduflow.repository.academic.TeacherRepository;
import com.eduflow.service.ClassService;
import com.eduflow.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final SchoolClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final StudentRepository studentRepository;
    private final StudentService studentService;

    @Override
    @Transactional
    public ClassResponse createClass(CreateClassRequest request) {
        if (classRepository.existsByNameAndGradeAndAcademicYear(
                request.getName(), request.getGrade(), request.getAcademicYear())) {
            throw new DuplicateResourceException("Class", "name/grade/academicYear",
                    request.getName() + "/" + request.getGrade() + "/" + request.getAcademicYear());
        }

        SchoolClass schoolClass = SchoolClass.builder()
                .name(request.getName())
                .grade(request.getGrade())
                .academicYear(request.getAcademicYear())
                .section(request.getSection())
                .maxCapacity(request.getMaxCapacity())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        if (request.getClassTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(request.getClassTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.getClassTeacherId()));
            schoolClass.setClassTeacher(teacher);
        }

        if (request.getSubjectIds() != null && !request.getSubjectIds().isEmpty()) {
            Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(request.getSubjectIds()));
            schoolClass.setSubjects(subjects);
        }

        schoolClass = classRepository.save(schoolClass);
        log.info("Class created: {} for academic year {}", schoolClass.getName(), schoolClass.getAcademicYear());

        return mapToResponse(schoolClass);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse getClassById(Long id) {
        SchoolClass schoolClass = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));
        return mapToResponse(schoolClass);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClassResponse> getAllClasses(Pageable pageable) {
        Page<SchoolClass> page = classRepository.findAll(pageable);
        List<ClassResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClassResponse> getClassesByFilters(String academicYear, Integer grade, Pageable pageable) {
        Page<SchoolClass> page;

        if (academicYear != null && grade != null) {
            page = classRepository.findByGradeAndAcademicYear(grade, academicYear, pageable);
        } else if (academicYear != null) {
            page = classRepository.findByAcademicYear(academicYear, pageable);
        } else if (grade != null) {
            page = classRepository.findByGrade(grade, pageable);
        } else {
            page = classRepository.findAll(pageable);
        }

        List<ClassResponse> content = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();
        return PagedResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional
    public ClassResponse updateClass(Long id, CreateClassRequest request) {
        SchoolClass schoolClass = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));

        // Check for duplicate if name, grade, or academic year changed
        if (!schoolClass.getName().equals(request.getName()) ||
                !schoolClass.getGrade().equals(request.getGrade()) ||
                !schoolClass.getAcademicYear().equals(request.getAcademicYear())) {

            if (classRepository.existsByNameAndGradeAndAcademicYear(
                    request.getName(), request.getGrade(), request.getAcademicYear())) {
                throw new DuplicateResourceException("Class", "name/grade/academicYear",
                        request.getName() + "/" + request.getGrade() + "/" + request.getAcademicYear());
            }
        }

        schoolClass.setName(request.getName());
        schoolClass.setGrade(request.getGrade());
        schoolClass.setAcademicYear(request.getAcademicYear());
        schoolClass.setSection(request.getSection());
        schoolClass.setMaxCapacity(request.getMaxCapacity());

        if (request.getActive() != null) {
            schoolClass.setActive(request.getActive());
        }

        if (request.getClassTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(request.getClassTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", request.getClassTeacherId()));
            schoolClass.setClassTeacher(teacher);
        } else {
            schoolClass.setClassTeacher(null);
        }

        if (request.getSubjectIds() != null) {
            Set<Subject> subjects = new HashSet<>(subjectRepository.findAllById(request.getSubjectIds()));
            schoolClass.setSubjects(subjects);
        }

        schoolClass = classRepository.save(schoolClass);
        log.info("Class updated: {}", schoolClass.getId());

        return mapToResponse(schoolClass);
    }

    @Override
    @Transactional
    public void deleteClass(Long id) {
        SchoolClass schoolClass = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", id));

        Long studentCount = classRepository.countStudentsByClassId(id);
        if (studentCount > 0) {
            throw new BadRequestException("Cannot delete class with " + studentCount + " enrolled students. Remove students first.");
        }

        schoolClass.setActive(false);
        classRepository.save(schoolClass);
        log.info("Class deactivated: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentResponse> getClassStudents(Long classId) {
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class", "id", classId);
        }
        return studentService.getStudentsByClassId(classId);
    }

    @Override
    @Transactional
    public StudentResponse assignStudentToClass(Long classId, Long studentId) {
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Class", "id", classId);
        }
        return studentService.enrollStudentInClass(studentId, classId);
    }

    @Override
    @Transactional
    public void removeStudentFromClass(Long classId, Long studentId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        if (student.getCurrentClass() == null || !student.getCurrentClass().getId().equals(classId)) {
            throw new BadRequestException("Student is not enrolled in this class");
        }

        student.setCurrentClass(null);
        studentRepository.save(student);
        log.info("Student {} removed from class {}", studentId, classId);
    }

    @Override
    @Transactional
    public ClassResponse addSubjectToClass(Long classId, Long subjectId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));

        schoolClass.addSubject(subject);
        schoolClass = classRepository.save(schoolClass);
        log.info("Subject {} added to class {}", subjectId, classId);

        return mapToResponse(schoolClass);
    }

    @Override
    @Transactional
    public ClassResponse removeSubjectFromClass(Long classId, Long subjectId) {
        SchoolClass schoolClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));

        schoolClass.removeSubject(subject);
        schoolClass = classRepository.save(schoolClass);
        log.info("Subject {} removed from class {}", subjectId, classId);

        return mapToResponse(schoolClass);
    }

    private ClassResponse mapToResponse(SchoolClass schoolClass) {
        ClassResponse.TeacherSummary teacherSummary = null;
        if (schoolClass.getClassTeacher() != null) {
            Teacher t = schoolClass.getClassTeacher();
            teacherSummary = ClassResponse.TeacherSummary.builder()
                    .id(t.getId())
                    .employeeId(t.getEmployeeId())
                    .name(t.getUser().getFullName())
                    .email(t.getUser().getEmail())
                    .build();
        }

        List<ClassResponse.SubjectSummary> subjectSummaries = schoolClass.getSubjects().stream()
                .map(s -> ClassResponse.SubjectSummary.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .code(s.getCode())
                        .build())
                .collect(Collectors.toList());

        Long studentCount = classRepository.countStudentsByClassId(schoolClass.getId());

        return ClassResponse.builder()
                .id(schoolClass.getId())
                .name(schoolClass.getName())
                .grade(schoolClass.getGrade())
                .academicYear(schoolClass.getAcademicYear())
                .section(schoolClass.getSection())
                .maxCapacity(schoolClass.getMaxCapacity())
                .currentEnrollment(studentCount.intValue())
                .active(schoolClass.getActive())
                .classTeacher(teacherSummary)
                .subjects(subjectSummaries)
                .build();
    }
}