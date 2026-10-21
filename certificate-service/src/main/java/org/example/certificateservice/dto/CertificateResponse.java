package org.example.certificateservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.certificateservice.model.Certificate;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ответ с данными созданного сертификата")
public class CertificateResponse {
    @Schema(description = "ID сертификата", example = "501")
    private Long id;
    @Schema(description = "ID записи на курс", example = "1")
    private Long enrollmentId;
    @Schema(description = "Статус сертификата", example = "GENERATED")
    private String status;
    @Schema(description = "Номер сертификата — по нему проверяют подлинность", example = "UN-2026-000001")
    private String number;
    @Schema(description = "Шаблон оформления", example = "Стандартный")
    private String templateName;
    @Schema(description = "Когда выдан")
    private LocalDateTime issuedAt;

    public static CertificateResponse from(Certificate c) {
        return new CertificateResponse(c.getId(), c.getEnrollmentId(), "GENERATED", c.getNumber(),
                c.getTemplate().getName(), c.getIssuedAt());
    }
}
