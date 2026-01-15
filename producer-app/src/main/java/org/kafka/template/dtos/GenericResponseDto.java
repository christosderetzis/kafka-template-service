package org.kafka.template.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.kafka.template.enums.GenericResponseStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GenericResponseDto<T> {
    private T data;
    private GenericResponseStatus status;
}
