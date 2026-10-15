package org.example.courseservice.controller;

import org.example.courseservice.dto.CourseDto;
import org.example.courseservice.dto.CourseRequest;
import org.example.courseservice.exception.ConflictException;
import org.example.courseservice.exception.NotFoundException;
import org.example.courseservice.model.CourseStatus;
import org.example.courseservice.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CourseController.class, InternalCourseController.class})
@ActiveProfiles("test")
class CourseControllerTest {

    private static final String VALID_COURSE = """
            {
              "title": "Spring Boot и микросервисы",
              "price": 14990.00,
              "maxStudents": 20,
              "startDate": "2026-11-02",
              "endDate": "2027-01-31",
              "categoryId": 1,
              "tagIds": [1, 2]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    private static CourseDto course(Long id, String title, CourseStatus status) {
        return new CourseDto(id, title, null, new BigDecimal("14990.00"), 20, status,
                LocalDate.of(2026, 11, 2), LocalDate.of(2027, 1, 31),
                1L, "Программирование", 1L, "Иванов Пётр Сергеевич", List.of("Java", "Spring"));
    }

    @Test
    void getAllReturnsCourses() throws Exception {
        when(courseService.findAll()).thenReturn(List.of(
                course(1L, "Java с нуля", CourseStatus.PUBLISHED),
                course(2L, "Spring Boot и микросервисы", CourseStatus.DRAFT)));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Java с нуля"))
                .andExpect(jsonPath("$[1].status").value("DRAFT"));
    }

    @Test
    void getByIdReturnsCourse() throws Exception {
        when(courseService.findById(1L)).thenReturn(course(1L, "Java с нуля", CourseStatus.PUBLISHED));

        mockMvc.perform(get("/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoryName").value("Программирование"))
                .andExpect(jsonPath("$.tags[1]").value("Spring"));
    }

    @Test
    void getByIdReturns404WhenCourseMissing() throws Exception {
        when(courseService.findById(99L)).thenThrow(new NotFoundException("Курс с id 99 не найден"));

        mockMvc.perform(get("/courses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Курс с id 99 не найден"));
    }

    @Test
    void getByIdReturns400WhenIdIsNotNumber() throws Exception {
        mockMvc.perform(get("/courses/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Некорректный запрос"));
    }

    @Test
    void createReturns201() throws Exception {
        when(courseService.create(any(CourseRequest.class)))
                .thenReturn(course(6L, "Spring Boot и микросервисы", CourseStatus.DRAFT));

        mockMvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON).content(VALID_COURSE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"title": " ", "price": -5, "maxStudents": 0, "startDate": "2026-12-01", "endDate": "2026-11-01"}
                """;

        mockMvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ошибка валидации"))
                .andExpect(jsonPath("$.errors.title").value("Название обязательно"))
                .andExpect(jsonPath("$.errors.price").value("Цена не может быть отрицательной"))
                .andExpect(jsonPath("$.errors.maxStudents").exists())
                .andExpect(jsonPath("$.errors.datesValid").value("Дата окончания не может быть раньше даты начала"));

        verify(courseService, never()).create(any());
    }

    @Test
    void createReturns400WhenJsonIsBroken() throws Exception {
        mockMvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON).content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Тело запроса не удалось прочитать"));
    }

    @Test
    void updateReturns404WhenCourseMissing() throws Exception {
        when(courseService.update(eq(99L), any(CourseRequest.class)))
                .thenThrow(new NotFoundException("Курс с id 99 не найден"));

        mockMvc.perform(put("/courses/99").contentType(MediaType.APPLICATION_JSON).content(VALID_COURSE))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/courses/1"))
                .andExpect(status().isNoContent());

        verify(courseService).delete(1L);
    }

    @Test
    void deleteReturns409WhenCourseHasModules() throws Exception {
        doThrow(new ConflictException("У курса 1 есть модули")).when(courseService).delete(1L);

        mockMvc.perform(delete("/courses/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Конфликт"));
    }

    @Test
    void internalEndpointForEnrollmentReturnsCourse() throws Exception {
        when(courseService.findById(1L)).thenReturn(course(1L, "Java с нуля", CourseStatus.PUBLISHED));

        mockMvc.perform(get("/api/v1/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.maxStudents").value(20));
    }

    @Test
    void unexpectedErrorReturns500() throws Exception {
        when(courseService.findAll()).thenThrow(new IllegalStateException("БД недоступна"));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Внутренняя ошибка"));
    }

    @Test
    void wrongMethodReturns405() throws Exception {
        mockMvc.perform(delete("/courses"))
                .andExpect(status().isMethodNotAllowed());
    }
}
