package ru.hpclab.hl.module1.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.hpclab.hl.module1.repository.AppointmentRepository;
import ru.hpclab.hl.module1.repository.DoctorRepository;
import ru.hpclab.hl.module1.repository.PatientRepository;
import ru.hpclab.hl.module1.service.statistics.ObservabilityService;

@Service
@RequiredArgsConstructor
public class DatabaseCleanupService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ObservabilityService observabilityService;

    @Transactional
    public void clear() {
        observabilityService.start("service.cleanup.clear");

        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
        patientRepository.deleteAll();

        observabilityService.stop("service.cleanup.clear");
    }
}
