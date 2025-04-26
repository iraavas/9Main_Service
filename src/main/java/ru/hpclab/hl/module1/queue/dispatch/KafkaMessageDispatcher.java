package ru.hpclab.hl.module1.queue.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.hpclab.hl.module1.dto.AppointmentDTO;
import ru.hpclab.hl.module1.dto.DoctorDTO;
import ru.hpclab.hl.module1.dto.PatientDTO;
import ru.hpclab.hl.module1.model.queue.EntityType;
import ru.hpclab.hl.module1.model.queue.KafkaOperationMessage;
import ru.hpclab.hl.module1.model.queue.OperationType;
import ru.hpclab.hl.module1.service.AppointmentService;
import ru.hpclab.hl.module1.service.DoctorService;
import ru.hpclab.hl.module1.service.PatientService;

@Component
@RequiredArgsConstructor
public class KafkaMessageDispatcher {

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final ObjectMapper objectMapper;

    public void dispatch(KafkaOperationMessage message) {
        EntityType entity = message.getEntity();
        OperationType operation = message.getOperation();
        JsonNode payload = message.getPayload();

        switch (entity) {
            case PATIENT -> handlePatient(operation, payload);
            case DOCTOR -> handleDoctor(operation, payload);
            case APPOINTMENT -> handleAppointment(operation, payload);
            default -> throw new IllegalArgumentException("Неизвестная сущность: " + entity);
        }
    }

    private void handlePatient(OperationType operation, JsonNode payload) {
        switch (operation) {
            case POST -> patientService.savePatient(deserialize(payload, PatientDTO.class));
            case PUT -> {
                Long id = payload.get("id").asLong();
                patientService.updatePatient(id, deserialize(payload, PatientDTO.class));
            }
            case DELETE -> {
                Long id = payload.get("id").asLong();
                patientService.deletePatient(id);
            }
            default -> throw new IllegalArgumentException("Операция не поддерживается для PATIENT: " + operation);
        }
    }

    private void handleDoctor(OperationType operation, JsonNode payload) {
        switch (operation) {
            case POST -> doctorService.saveDoctor(deserialize(payload, DoctorDTO.class));
            case PUT -> {
                Long id = payload.get("id").asLong();
                doctorService.updateDoctor(id, deserialize(payload, DoctorDTO.class));
            }
            case DELETE -> {
                Long id = payload.get("id").asLong();
                doctorService.deleteDoctor(id);
            }
            default -> throw new IllegalArgumentException("Операция не поддерживается для DOCTOR: " + operation);
        }
    }

    private void handleAppointment(OperationType operation, JsonNode payload) {
        switch (operation) {
            case POST -> appointmentService.saveAppointment(deserialize(payload, AppointmentDTO.class));
            case PUT -> {
                Long id = payload.get("id").asLong();
                appointmentService.updateAppointment(id, deserialize(payload, AppointmentDTO.class));
            }
            case DELETE -> {
                Long id = payload.get("id").asLong();
                appointmentService.deleteAppointment(id);
            }
            default -> throw new IllegalArgumentException("Операция не поддерживается для APPOINTMENT: " + operation);
        }
    }

    private <T> T deserialize(JsonNode node, Class<T> clazz) {
        try {
            return objectMapper.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка десериализации payload в " + clazz.getSimpleName(), e);
        }
    }
}
