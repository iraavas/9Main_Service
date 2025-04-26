package ru.hpclab.hl.module1.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.hpclab.hl.module1.queue.dispatch.KafkaMessageDispatcher;
import ru.hpclab.hl.module1.service.AppointmentService;
import ru.hpclab.hl.module1.service.DoctorService;
import ru.hpclab.hl.module1.service.PatientService;


@Configuration
public class KafkaDispatcherConfig {
    @Bean
    public KafkaMessageDispatcher kafkaMessageDispatcher(
            PatientService patientService,
            DoctorService doctorService,
            AppointmentService appointmentService,
            ObjectMapper objectMapper
    ) {
        return new KafkaMessageDispatcher(
                patientService,
                doctorService,
                appointmentService,
                objectMapper
        );
    }
}