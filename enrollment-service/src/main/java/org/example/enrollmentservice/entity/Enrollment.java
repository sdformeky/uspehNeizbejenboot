package org.example.enrollmentservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@Schema(description = "Сущность записи студента на курс")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Уникальный идентификатор записи", example = "1")
    private Long id;

    @Schema(description = "Идентификатор студента (валидируется в student-service)", example = "42")
    private Long studentId;

    @Schema(description = "Идентификатор курса (валидируется в course-service)", example = "105")
    private Long courseId;
}
