package org.example.courseservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.courseservice.dto.CourseDto;
import org.example.courseservice.dto.CourseRequest;
import org.example.courseservice.dto.LessonViewDto;
import org.example.courseservice.exception.ConflictException;
import org.example.courseservice.exception.NotFoundException;
import org.example.courseservice.model.Category;
import org.example.courseservice.model.Course;
import org.example.courseservice.model.CourseModule;
import org.example.courseservice.model.CourseStatus;
import org.example.courseservice.model.Instructor;
import org.example.courseservice.model.Lesson;
import org.example.courseservice.model.Tag;
import org.example.courseservice.repository.CategoryRepository;
import org.example.courseservice.repository.CourseModuleRepository;
import org.example.courseservice.repository.CourseRepository;
import org.example.courseservice.repository.InstructorRepository;
import org.example.courseservice.repository.LessonRepository;
import org.example.courseservice.repository.ReviewRepository;
import org.example.courseservice.repository.TagRepository;
import org.example.courseservice.repository.WebinarRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final InstructorRepository instructorRepository;
    private final TagRepository tagRepository;
    private final CourseModuleRepository moduleRepository;
    private final ReviewRepository reviewRepository;
    private final WebinarRepository webinarRepository;
    private final LessonRepository lessonRepository;

    public List<CourseDto> findAll() {
        return courseRepository.findAll(Sort.by("id")).stream()
                .map(CourseDto::from)
                .toList();
    }

    public CourseDto findById(Long id) {
        return CourseDto.from(getCourse(id));
    }

    public LessonViewDto findLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new NotFoundException("Урок с id " + lessonId + " не найден"));
        CourseModule module = lesson.getModule();
        Course course = module.getCourse();
        List<Long> ids = lessonRepository.findIdsByCourseId(course.getId());
        int index = ids.indexOf(lessonId);
        return new LessonViewDto(
                lesson.getId(),
                course.getId(),
                course.getTitle(),
                module.getPosition(),
                module.getTitle(),
                index + 1,
                ids.size(),
                lesson.getTitle(),
                lesson.getContent(),
                lesson.getDurationMin(),
                lesson.getVideoUrl() != null,
                index > 0 ? ids.get(index - 1) : null,
                index < ids.size() - 1 ? ids.get(index + 1) : null
        );
    }

    public List<Long> findLessonIds(Long id) {
        getCourse(id);
        return lessonRepository.findIdsByCourseId(id);
    }

    @Transactional
    public CourseDto create(CourseRequest request) {
        Course course = new Course();
        apply(course, request);
        Course saved = courseRepository.save(course);
        log.info("Создан курс id={} «{}», статус {}", saved.getId(), saved.getTitle(), saved.getStatus());
        return CourseDto.from(saved);
    }

    @Transactional
    public CourseDto update(Long id, CourseRequest request) {
        Course course = getCourse(id);
        apply(course, request);
        log.info("Обновлён курс id={} «{}», статус {}", id, course.getTitle(), course.getStatus());
        return CourseDto.from(course);
    }

    @Transactional
    public void delete(Long id) {
        Course course = getCourse(id);
        if (moduleRepository.existsByCourseId(id) || reviewRepository.existsByCourseId(id)
                || webinarRepository.existsByCourseId(id)) {
            throw new ConflictException("У курса " + id + " есть модули, отзывы или вебинары — удалите их "
                    + "или переведите курс в архив");
        }
        courseRepository.delete(course);
        log.info("Удалён курс id={} «{}»", id, course.getTitle());
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Курс с id " + id + " не найден"));
    }

    private void apply(Course course, CourseRequest request) {
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("Категория с id " + request.categoryId() + " не найдена"));
        }
        Instructor instructor = null;
        if (request.instructorId() != null) {
            instructor = instructorRepository.findById(request.instructorId())
                    .orElseThrow(() -> new NotFoundException("Преподаватель с id " + request.instructorId() + " не найден"));
        }
        Set<Tag> tags = new HashSet<>();
        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            tags.addAll(tagRepository.findAllById(request.tagIds()));
            if (tags.size() != request.tagIds().size()) {
                Set<Long> missing = new HashSet<>(request.tagIds());
                tags.forEach(tag -> missing.remove(tag.getId()));
                throw new NotFoundException("Теги не найдены: " + missing);
            }
        }
        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        course.setPrice(request.price());
        course.setMaxStudents(request.maxStudents());
        course.setStatus(request.status() != null ? request.status() : CourseStatus.DRAFT);
        course.setStartDate(request.startDate());
        course.setEndDate(request.endDate());
        course.setCategory(category);
        course.setInstructor(instructor);
        course.getTags().clear();
        course.getTags().addAll(tags);
    }
}
