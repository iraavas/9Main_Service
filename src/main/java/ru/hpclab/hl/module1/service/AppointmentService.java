package ru.hpclab.hl.module1.service;

import org.springframework.stereotype.Service;
import ru.hpclab.hl.module1.controller.exception.DoctorException;
import ru.hpclab.hl.module1.dto.AppointmentDTO;
import ru.hpclab.hl.module1.entity.AppointmentEntity;
import ru.hpclab.hl.module1.entity.DoctorEntity;
import ru.hpclab.hl.module1.entity.PatientEntity;
import ru.hpclab.hl.module1.mapper.AppointmentMapper;
import ru.hpclab.hl.module1.repository.AppointmentRepository;
import ru.hpclab.hl.module1.repository.DoctorRepository;
import ru.hpclab.hl.module1.repository.PatientRepository;
import ru.hpclab.hl.module1.model.AppointmentStatus;
import ru.hpclab.hl.module1.service.statistics.ObservabilityService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ObservabilityService observabilityService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              ObservabilityService observabilityService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.observabilityService = observabilityService;
    }

    public List<AppointmentDTO> getAllAppointments() {
        observabilityService.start("service.appointment.getAll");
        List<AppointmentDTO> result = appointmentRepository.findAll().stream()
                .map(AppointmentMapper::toDTO)
                .collect(Collectors.toList());
        observabilityService.stop("service.appointment.getAll");
        return result;
    }

    public AppointmentDTO getAppointmentById(Long id) {
        observabilityService.start("service.appointment.getById");
        Optional<AppointmentEntity> appointmentEntity = appointmentRepository.findById(id);
        AppointmentDTO result = appointmentEntity.map(AppointmentMapper::toDTO).orElse(null);
        observabilityService.stop("service.appointment.getById");
        return result;
    }

    public AppointmentDTO saveAppointment(AppointmentDTO appointmentDTO) {
        observabilityService.start("service.appointment.save");

        PatientEntity patient = patientRepository.findById(appointmentDTO.getPatientId())
                .orElseThrow(() -> new RuntimeException("Пациент не найден"));

        DoctorEntity doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Доктор не найден"));

        Long count = appointmentRepository.countAppointmentsForSpecializationAtTime(
                appointmentDTO.getSpecialization(), appointmentDTO.getAppointmentDate());
        if (count > 0) {
            observabilityService.stop("service.appointment.save");
            throw new DoctorException("Врач с специализацией " + appointmentDTO.getSpecialization() +
                    " уже занят в это время: " + appointmentDTO.getAppointmentDate());
        }

        AppointmentEntity appointmentEntity = AppointmentMapper.toEntity(appointmentDTO, patient, doctor);
        appointmentEntity.setStatus(AppointmentStatus.SCHEDULED);
        AppointmentDTO result = AppointmentMapper.toDTO(appointmentRepository.save(appointmentEntity));

        observabilityService.stop("service.appointment.save");
        return result;
    }

    public AppointmentDTO updateAppointment(Long id, AppointmentDTO appointmentDTO) {
        observabilityService.start("service.appointment.update");
        AppointmentDTO result = appointmentRepository.findById(id)
                .map(existingAppointment -> {
                    PatientEntity patient = patientRepository.findById(appointmentDTO.getPatientId())
                            .orElseThrow(() -> new RuntimeException("Пациент не найден"));

                    DoctorEntity doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                            .orElseThrow(() -> new RuntimeException("Доктор не найден"));

                    existingAppointment.setPatient(patient);
                    existingAppointment.setDoctor(doctor);
                    existingAppointment.setAppointmentDate(appointmentDTO.getAppointmentDate());
                    existingAppointment.setDiagnosis(appointmentDTO.getDiagnosis());

                    return AppointmentMapper.toDTO(appointmentRepository.save(existingAppointment));
                })
                .orElse(null);
        observabilityService.stop("service.appointment.update");
        return result;
    }

    public void deleteAppointment(Long id) {
        observabilityService.start("service.appointment.delete");
        appointmentRepository.deleteById(id);
        observabilityService.stop("service.appointment.delete");
    }
}
