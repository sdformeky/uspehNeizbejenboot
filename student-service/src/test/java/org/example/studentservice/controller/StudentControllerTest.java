package org.example.studentservice.controller;

import org.example.studentservice.dto.StudentDto;
import org.example.studentservice.dto.StudentRequest;
import org.example.studentservice.exception.ConflictException;
import org.example.studentservice.exception.NotFoundException;
import org.example.studentservice.model.StudentStatus;
import org.example.studentservice.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

@WebMvcTest(StudentController.class)
@ActiveProfiles("test")
class StudentControllerTest {

    private static final String VALID_STUDENT = """
            {
              "firstName": "Алексей",
              "lastName": "Кузнецов",
              "phone": "+79161234567",
              "birthDate": "2005-03-14",
              "groupId": 1
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    private static StudentDto student(Long id, String firstName, StudentStatus status) {
        return new StudentDto(id, null, 1L, "ИСП-21", firstName, "Кузнецов", "+79161234567",
                LocalDate.of(2005, 3, 14), status, LocalDateTime.of(2026, 10, 3, 11, 17));
    }

    @Test
    void getAllReturnsStudents() throws Exception {
        when(studentService.findAll()).thenReturn(List.of(
                student(1L, "Алексей", StudentStatus.ACTIVE),
                student(2L, "Мария", StudentStatus.BLOCKED)));

        mockMvc.perform(get("/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").value("Алексей"))
                .andExpect(jsonPath("$[1].status").value("BLOCKED"));
    }

    @Test
    void getByIdReturnsStudent() throws Exception {
        when(studentService.findById(1L)).thenReturn(student(1L, "Алексей", StudentStatus.ACTIVE));

        mockMvc.perform(get("/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.groupName").value("ИСП-21"));
    }

    @Test
    void getByIdReturns404WhenStudentMissing() throws Exception {
        when(studentService.findById(99L)).thenThrow(new NotFoundException("Студент с id 99 не найден"));

        mockMvc.perform(get("/students/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Студент с id 99 не найден"));
    }

    @Test
    void getByIdReturns400WhenIdIsNotNumber() throws Exception {
        mockMvc.perform(get("/students/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Некорректный запрос"));
    }

    @Test
    void createReturns201() throws Exception {
        when(studentService.create(any(StudentRequest.class))).thenReturn(student(6L, "Алексей", StudentStatus.ACTIVE));

        mockMvc.perform(post("/students").contentType(MediaType.APPLICATION_JSON).content(VALID_STUDENT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(6))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createReturns400WithFieldErrors() throws Exception {
        String invalid = """
                {"firstName": "", "phone": "12", "birthDate": "2099-01-01"}
                """;

        mockMvc.perform(post("/students").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ошибка валидации"))
                .andExpect(jsonPath("$.errors.firstName").value("Имя обязательно"))
                .andExpect(jsonPath("$.errors.lastName").value("Фамилия обязательна"))
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.birthDate").value("Дата рождения должна быть в прошлом"));

        verify(studentService, never()).create(any());
    }

    @Test
    void createReturns400WhenJsonIsBroken() throws Exception {
        mockMvc.perform(post("/students").contentType(MediaType.APPLICATION_JSON).content("{\"firstName\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Тело запроса не удалось прочитать"));
    }

    @Test
    void createReturns409WhenUserIdTaken() throws Exception {
        when(studentService.create(any(StudentRequest.class)))
                .thenThrow(new ConflictException("Пользователь 15 уже привязан к студенту 1"));

        mockMvc.perform(post("/students").contentType(MediaType.APPLICATION_JSON).content(VALID_STUDENT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Конфликт"));
    }

    @Test
    void updateReturns404WhenStudentMissing() throws Exception {
        when(studentService.update(eq(99L), any(StudentRequest.class)))
                .thenThrow(new NotFoundException("Студент с id 99 не найден"));

        mockMvc.perform(put("/students/99").contentType(MediaType.APPLICATION_JSON).content(VALID_STUDENT))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/students/1"))
                .andExpect(status().isNoContent());

        verify(studentService).delete(1L);
    }

    @Test
    void deleteReturns404WhenStudentMissing() throws Exception {
        doThrow(new NotFoundException("Студент с id 99 не найден")).when(studentService).delete(99L);

        mockMvc.perform(delete("/students/99"))
                .andExpect(status().isNotFound());
    }


    @Test
    void unexpectedErrorReturns500() throws Exception {
        when(studentService.findAll()).thenThrow(new IllegalStateException("БД недоступна"));

        mockMvc.perform(get("/students"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Внутренняя ошибка"));
    }

    @Test
    void wrongMethodReturns405() throws Exception {
        mockMvc.perform(delete("/students"))
                .andExpect(status().isMethodNotAllowed());
    }
}
