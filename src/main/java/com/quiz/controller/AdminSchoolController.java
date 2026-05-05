package com.quiz.controller;

import com.quiz.entity.ClassRoster;
import com.quiz.entity.GradeLevel;
import com.quiz.entity.SchoolClass;
import com.quiz.entity.SchoolYear;
import com.quiz.entity.Subject;
import com.quiz.entity.TeacherAssignment;
import com.quiz.entity.User;
import com.quiz.exception.ApiException;
import com.quiz.repository.ClassRosterRepository;
import com.quiz.repository.GradeLevelRepository;
import com.quiz.repository.SchoolClassRepository;
import com.quiz.repository.SchoolYearRepository;
import com.quiz.repository.SubjectRepository;
import com.quiz.repository.TeacherAssignmentRepository;
import com.quiz.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/school")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSchoolController {

    private final SchoolYearRepository schoolYearRepository;
    private final GradeLevelRepository gradeLevelRepository;
    private final SubjectRepository subjectRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassRosterRepository classRosterRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final UserRepository userRepository;

    public record SchoolYearBody(
            @NotBlank @Size(max = 120) String name,
            LocalDate startsOn,
            LocalDate endsOn
    ) {
    }

    @GetMapping("/years")
    public List<SchoolYear> listYears() {
        return schoolYearRepository.findAll();
    }

    @PostMapping("/years")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolYear createYear(@Valid @RequestBody SchoolYearBody body) {
        SchoolYear y = SchoolYear.builder()
                .name(body.name().trim())
                .startsOn(body.startsOn())
                .endsOn(body.endsOn())
                .build();
        return schoolYearRepository.save(y);
    }

    public record GradeBody(@NotBlank @Size(max = 32) String code, @NotBlank @Size(max = 120) String name) {
    }

    @GetMapping("/grades")
    public List<GradeLevel> listGrades() {
        return gradeLevelRepository.findAll();
    }

    @PostMapping("/grades")
    @ResponseStatus(HttpStatus.CREATED)
    public GradeLevel createGrade(@Valid @RequestBody GradeBody body) {
        if (gradeLevelRepository.findByCode(body.code().trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Grade code already exists");
        }
        return gradeLevelRepository.save(GradeLevel.builder()
                .code(body.code().trim())
                .name(body.name().trim())
                .build());
    }

    public record SubjectBody(@NotBlank @Size(max = 32) String code, @NotBlank @Size(max = 200) String name) {
    }

    @GetMapping("/subjects")
    public List<Subject> listSubjects() {
        return subjectRepository.findAll();
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public Subject createSubject(@Valid @RequestBody SubjectBody body) {
        if (subjectRepository.findByCode(body.code().trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Subject code already exists");
        }
        return subjectRepository.save(Subject.builder()
                .code(body.code().trim())
                .name(body.name().trim())
                .build());
    }

    public record ClassBody(
            @NotBlank @Size(max = 120) String name,
            Long schoolYearId,
            Long gradeLevelId
    ) {
    }

    @GetMapping("/classes")
    public List<SchoolClass> listClasses() {
        return schoolClassRepository.findAll();
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolClass createClass(@Valid @RequestBody ClassBody body) {
        SchoolYear year = body.schoolYearId() == null ? null
                : schoolYearRepository.findById(body.schoolYearId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown school year"));
        GradeLevel grade = body.gradeLevelId() == null ? null
                : gradeLevelRepository.findById(body.gradeLevelId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown grade level"));
        return schoolClassRepository.save(SchoolClass.builder()
                .name(body.name().trim())
                .schoolYear(year)
                .gradeLevel(grade)
                .build());
    }

    public record RosterBody(@NotNull Long schoolClassId, @NotNull Long studentUserId) {
    }

    @PostMapping("/roster")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassRoster addRoster(@Valid @RequestBody RosterBody body) {
        SchoolClass sc = schoolClassRepository.findById(body.schoolClassId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown class"));
        User student = userRepository.findById(body.studentUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown student user"));
        return classRosterRepository.save(ClassRoster.builder().schoolClass(sc).student(student).build());
    }

    @DeleteMapping("/roster/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoster(@PathVariable Long id) {
        classRosterRepository.deleteById(id);
    }

    public record AssignmentBody(
            @NotNull Long teacherUserId,
            @NotNull Long subjectId,
            @NotNull Long schoolClassId
    ) {
    }

    @PostMapping("/teacher-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public TeacherAssignment addAssignment(@Valid @RequestBody AssignmentBody body) {
        User teacher = userRepository.findById(body.teacherUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown teacher"));
        Subject subject = subjectRepository.findById(body.subjectId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown subject"));
        SchoolClass sc = schoolClassRepository.findById(body.schoolClassId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown class"));
        return teacherAssignmentRepository.save(TeacherAssignment.builder()
                .teacher(teacher)
                .subject(subject)
                .schoolClass(sc)
                .build());
    }
}
