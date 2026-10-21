package org.example.certificateservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
