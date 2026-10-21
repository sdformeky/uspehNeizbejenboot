package org.example.courseservice.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.courseservice.client.EnrollmentClient;
import org.example.courseservice.client.StudentClient;
import org.example.courseservice.dto.CourseDto;
import org.example.courseservice.dto.CoursePageDto;
import org.example.courseservice.dto.CoursePageDto.LessonDto;
import org.example.courseservice.dto.CoursePageDto.ModuleDto;
import org.example.courseservice.dto.CoursePageDto.PostDto;
import org.example.courseservice.dto.CoursePageDto.ReviewDto;
import org.example.courseservice.dto.CoursePageDto.WebinarDto;
import org.example.courseservice.dto.EnrollmentShortDto;
import org.example.courseservice.dto.PostRequest;
import org.example.courseservice.dto.ReviewRequest;
import org.example.courseservice.dto.StudentShortDto;
import org.example.courseservice.exception.ConflictException;
import org.example.courseservice.exception.ForbiddenException;
import org.example.courseservice.exception.NotFoundException;
import org.example.courseservice.exception.ServiceUnavailableException;
import org.example.courseservice.model.Course;
import org.example.courseservice.model.CourseModule;
import org.example.courseservice.model.CoursePost;
import org.example.courseservice.model.Lesson;
import org.example.courseservice.model.Review;
import org.example.courseservice.repository.CourseModuleRepository;
import org.example.courseservice.repository.CoursePostRepository;
import org.example.courseservice.repository.CourseRepository;
import org.example.courseservice.repository.LessonRepository;
import org.example.courseservice.repository.ReviewRepository;
import org.example.courseservice.repository.WebinarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoursePageService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final WebinarRepository webinarRepository;
    private final CoursePostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final StudentClient studentClient;
    private final EnrollmentClient enrollmentClient;

    public CoursePageDto getPage(Long courseId) {
        Course course = getCourse(courseId);

        // программа: модули по порядку, в каждом — уроки по порядку
        List<ModuleDto> modules = new ArrayList<>();
        int lessonsCount = 0;
        int totalMinutes = 0;
        for (CourseModule module : moduleRepository.findByCourseIdOrderByPositionAsc(courseId)) {
            List<LessonDto> lessons = new ArrayList<>();
            for (Lesson lesson : lessonRepository.findByModuleIdOrderByPositionAsc(module.getId())) {
                lessons.add(new LessonDto(lesson.getId(), lesson.getTitle(), lesson.getDurationMin(),
                        lesson.getVideoUrl() != null));
                lessonsCount++;
                if (lesson.getDurationMin() != null) {
                    totalMinutes += lesson.getDurationMin();
                }
            }
            modules.add(new ModuleDto(module.getId(), module.getTitle(), module.getPosition(), lessons));
        }

        List<WebinarDto> webinars = webinarRepository.findByCourseIdOrderByStartsAtAsc(courseId).stream()
                .map(w -> new WebinarDto(w.getId(), w.getTitle(), w.getStartsAt(), w.getLink()))
                .toList();

        List<PostDto> posts = postRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(p -> new PostDto(p.getId(), p.getTitle(), p.getText(), p.getAuthorName(), p.getCreatedAt()))
                .toList();

        // отзывы: имена авторов берём из student-service; если он недоступен — страница всё равно открывается
        List<Review> reviewList = reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        Map<Long, String> names = studentNames();
        List<ReviewDto> reviews = new ArrayList<>();
        int ratingSum = 0;
        for (Review r : reviewList) {
            String name = names.getOrDefault(r.getStudentId(), "Студент");
            reviews.add(new ReviewDto(r.getId(), r.getStudentId(), name, r.getRating(), r.getComment(), r.getCreatedAt()));
            ratingSum += r.getRating();
        }
        Double average = reviewList.isEmpty() ? null : Math.round(ratingSum * 10.0 / reviewList.size()) / 10.0;

        String bio = course.getInstructor() != null ? course.getInstructor().getBio() : null;
        return new CoursePageDto(CourseDto.from(course), bio, modules, lessonsCount, totalMinutes,
                webinars, posts, reviews, average);
    }

    @Transactional
    public PostDto addPost(Long courseId, PostRequest request) {
        Course course = getCourse(courseId);
        CoursePost post = new CoursePost();
        post.setCourse(course);
        post.setTitle(request.title().trim());
        post.setText(request.text().trim());
        post.setAuthorName(course.getInstructor() != null ? course.getInstructor().getFullName() : "Администрация школы");
        postRepository.save(post);
        log.info("Новый пост в курсе {}: {}", courseId, post.getTitle());
        return new PostDto(post.getId(), post.getTitle(), post.getText(), post.getAuthorName(), post.getCreatedAt());
    }

    // Отзыв оставляет только записанный на курс: профиль студента ищем по пользователю из токена (student-service),
    // запись на курс проверяем в enrollment-service
    @Transactional
    public ReviewDto addReview(Long courseId, ReviewRequest request, Long currentUserId) {
        Course course = getCourse(courseId);
        if (currentUserId == null) {
            throw new ForbiddenException("Нужно войти");
        }
        List<StudentShortDto> profile;
        List<EnrollmentShortDto> enrollments;
        try {
            profile = studentClient.getByUserId(currentUserId);
            if (profile.isEmpty()) {
                throw new ForbiddenException("Сначала заполните профиль студента");
            }
            enrollments = enrollmentClient.getByStudent(profile.get(0).id());
        } catch (FeignException e) {
            log.warn("Не удалось проверить запись на курс {}: {}", courseId, e.getMessage());
            throw new ServiceUnavailableException("Не удалось проверить запись на курс: сервис студентов или записей недоступен, попробуйте позже");
        }
        StudentShortDto student = profile.get(0);

        boolean enrolled = false;
        for (EnrollmentShortDto e : enrollments) {
            if (e.courseId().equals(courseId)) {
                enrolled = true;
            }
        }
        if (!enrolled) {
            throw new ForbiddenException("Отзыв могут оставить только записанные на курс");
        }
        if (reviewRepository.existsByCourseIdAndStudentId(courseId, student.id())) {
            throw new ConflictException("Вы уже оставили отзыв на этот курс");
        }

        Review review = new Review();
        review.setCourse(course);
        review.setStudentId(student.id());
        review.setRating(request.rating());
        review.setComment(request.comment() != null ? request.comment().trim() : null);
        reviewRepository.save(review);
        log.info("Отзыв на курс {} от студента {}: {}/5", courseId, student.id(), request.rating());
        return new ReviewDto(review.getId(), student.id(), shortName(student),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Курс с id " + id + " не найден"));
    }

    // «Ваня И.» — имя и первая буква фамилии
    private String shortName(StudentShortDto s) {
        return s.firstName() + " " + s.lastName().charAt(0) + ".";
    }

    private Map<Long, String> studentNames() {
        Map<Long, String> names = new HashMap<>();
        try {
            for (StudentShortDto s : studentClient.getAll()) {
                names.put(s.id(), shortName(s));
            }
        } catch (Exception e) {
            log.warn("student-service недоступен, имена в отзывах не показываем: {}", e.getMessage());
        }
        return names;
    }
}
