package ru.hpclab.hl.module1.model.queue;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

/**
 * Класс представляет структуру сообщения, передаваемого через Kafka.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaOperationMessage {
    private EntityType entity;       // Тип сущности: PATIENT, DOCTOR, APPOINTMENT
    private OperationType operation; // Тип операции: POST, PUT, DELETE
    private JsonNode payload;         // Содержимое сообщения (гибкий JSON)
}
