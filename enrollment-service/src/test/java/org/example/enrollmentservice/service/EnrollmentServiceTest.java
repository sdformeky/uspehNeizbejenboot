package org.example.enrollmentservice.service;

import org.example.enrollmentservice.client.CertificateClient;
import org.example.enrollmentservice.client.CourseClient;
import org.example.enrollmentservice.client.NotificationClient;
import org.example.enrollmentservice.client.StudentClient;
import org.example.enrollmentservice.dto.CourseDto;
import org.example.enrollmentservice.dto.StudentDto;
import org.example.enrollmentservice.exception.ConflictException;
import org.example.enrollmentservice.exception.ResourceNotFoundException;
import org.example.enrollmentservice.model.Enrollment;
import org.example.enrollmentservice.model.EnrollmentStatus;
import org.example.enrollmentservice.model.LessonProgress;
import org.example.enrollmentservice.repository.EnrollmentRepository;
import org.example.enrollmentservice.repository.LessonProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private StudentClient studentClient;
    @Mock
    private CourseClient courseClient;
    @Mock
    private CertificateClient certificateClient;
    @Mock
    private NotificationClient notificationClient;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private static StudentDto student(String status) {
        StudentDto student = new StudentDto();
        student.setId(1L);
        student.setFirstName("Алексей");
        student.setLastName("Кузнецов");
        student.setStatus(status);
        return student;
    }

    private static CourseDto course(String status, Integer maxStudents) {
        CourseDto course = new CourseDto();
        course.setId(2L);
        course.setTitle("Java с нуля");
        course.setStatus(status);
        course.setMaxStudents(maxStudents);
        return course;
    }

    private void saveReturnsWithId() {
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            enrollment.setId(10L);
            return enrollment;
        });
    }

    @Test
    void activeStudentEnrollsToPublishedCourse() {
        when(studentClient.getStudentById(1L)).thenReturn(student("ACTIVE"));
        when(courseClient.getCourseById(2L)).thenReturn(course("PUBLISHED", 20));
        when(enrollmentRepository.countByCourseIdAndStatusNot(2L, EnrollmentStatus.CANCELLED)).thenReturn(5L);
        saveReturnsWithId();

        Enrollment enrollment = enrollmentService.createEnrollment(1L, 2L, null, null);

        assertThat(enrollment.getId()).isEqualTo(10L);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(certificateClient, never()).issueCertificate(anyLong());
    }

    @Test
    void blockedStudentCannotEnroll() {
        when(studentClient.getStudentById(1L)).thenReturn(student("BLOCKED"));

        assertThatThrownBy(() -> enrollmentService.createEnrollment(1L, 2L, null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("заблокирован");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void cannotEnrollToDraftCourse() {
        when(studentClient.getStudentById(1L)).thenReturn(student("ACTIVE"));
        when(courseClient.getCourseById(2L)).thenReturn(course("DRAFT", null));

        assertThatThrownBy(() -> enrollmentService.createEnrollment(1L, 2L, null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("не открыт для записи");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void cannotEnrollToArchivedCourse() {
        when(studentClient.getStudentById(1L)).thenReturn(student("ACTIVE"));
        when(courseClient.getCourseById(2L)).thenReturn(course("ARCHIVED", null));

        assertThatThrownBy(() -> enrollmentService.createEnrollment(1L, 2L, null, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotEnrollWhenNoSeatsLeft() {
        when(studentClient.getStudentById(1L)).thenReturn(student("ACTIVE"));
        when(courseClient.getCourseById(2L)).thenReturn(course("PUBLISHED", 3));
        when(enrollmentRepository.countByCourseIdAndStatusNot(2L, EnrollmentStatus.CANCELLED)).thenReturn(3L);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(1L, 2L, null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("нет свободных мест");
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void courseWithoutLimitDoesNotCountSeats() {
        when(studentClient.getStudentById(1L)).thenReturn(student("ACTIVE"));
        when(courseClient.getCourseById(2L)).thenReturn(course("PUBLISHED", null));
        saveReturnsWithId();

        enrollmentService.createEnrollment(1L, 2L, null, null);

        verify(enrollmentRepository, never()).countByCourseIdAndStatusNot(any(), any());
    }

    @Test
    void repeatedEnrollmentIsConflict() {
        when(studentClient.getStudentById(1L)).thenReturn(student("ACTIVE"));
        when(courseClient.getCourseById(2L)).thenReturn(course("PUBLISHED", 20));
        when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.createEnrollment(1L, 2L, null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("уже записан");
    }

    private Enrollment activeEnrollment() {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(10L);
        enrollment.setStudentId(1L);
        enrollment.setCourseId(2L);
        return enrollment;
    }

    private List<LessonProgress> progressStore(Enrollment enrollment) {
        List<LessonProgress> store = new ArrayList<>();
        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrollment));
        when(studentClient.getStudentById(1L)).thenReturn(student("ACTIVE"));
        lenient().when(lessonProgressRepository.findByEnrollmentIdAndLessonId(any(), any())).thenAnswer(invocation ->
                store.stream().filter(p -> p.getLessonId().equals(invocation.getArgument(1))).findFirst());
        lenient().when(lessonProgressRepository.save(any(LessonProgress.class))).thenAnswer(invocation -> {
            store.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        lenient().when(lessonProgressRepository.findByEnrollmentId(10L)).thenReturn(store);
        lenient().when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return store;
    }

    @Test
    void completingLessonRaisesProgress() {
        Enrollment enrollment = activeEnrollment();
        progressStore(enrollment);
        when(courseClient.getLessonIds(2L)).thenReturn(List.of(101L, 102L, 103L, 104L));

        Enrollment result = enrollmentService.completeLesson(10L, 101L, null, null);

        assertThat(result.getProgress()).isEqualTo(25);
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        verify(certificateClient, never()).issueCertificate(anyLong());
    }

    @Test
    void lastLessonCompletesCourseAndIssuesCertificate() {
        Enrollment enrollment = activeEnrollment();
        progressStore(enrollment);
        when(courseClient.getLessonIds(2L)).thenReturn(List.of(101L, 102L));

        enrollmentService.completeLesson(10L, 101L, null, null);
        Enrollment result = enrollmentService.completeLesson(10L, 102L, null, null);

        assertThat(result.getProgress()).isEqualTo(100);
        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(certificateClient).issueCertificate(10L);
    }

    @Test
    void repeatedLessonIsCountedOnce() {
        Enrollment enrollment = activeEnrollment();
        List<LessonProgress> store = progressStore(enrollment);
        when(courseClient.getLessonIds(2L)).thenReturn(List.of(101L, 102L));

        enrollmentService.completeLesson(10L, 101L, null, null);
        Enrollment result = enrollmentService.completeLesson(10L, 101L, null, null);

        assertThat(store).hasSize(1);
        assertThat(result.getProgress()).isEqualTo(50);
    }

    @Test
    void lessonFromAnotherCourseIsNotFound() {
        progressStore(activeEnrollment());
        when(courseClient.getLessonIds(2L)).thenReturn(List.of(101L, 102L));

        assertThatThrownBy(() -> enrollmentService.completeLesson(10L, 999L, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void completedEnrollmentCannotBeChanged() {
        Enrollment enrollment = activeEnrollment();
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        progressStore(enrollment);

        assertThatThrownBy(() -> enrollmentService.completeLesson(10L, 101L, null, null))
                .isInstanceOf(ConflictException.class);
    }
}
