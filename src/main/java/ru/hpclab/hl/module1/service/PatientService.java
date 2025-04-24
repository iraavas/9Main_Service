package ru.hpclab.hl.module1.service;

import org.springframework.stereotype.Service;
import ru.hpclab.hl.module1.dto.PatientDTO;
import ru.hpclab.hl.module1.entity.PatientEntity;
import ru.hpclab.hl.module1.mapper.PatientMapper;
import ru.hpclab.hl.module1.repository.PatientRepository;
import ru.hpclab.hl.module1.service.statistics.ObservabilityService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final ObservabilityService observabilityService;

    public PatientService(PatientRepository patientRepository,
                          ObservabilityService observabilityService) {
        this.patientRepository = patientRepository;
        this.observabilityService = observabilityService;
    }

    public List<PatientDTO> getAllPatients() {
        observabilityService.start("service.patient.getAll");
        List<PatientDTO> result = patientRepository.findAll().stream()
                .map(PatientMapper::toDTO)
                .collect(Collectors.toList());
        observabilityService.stop("service.patient.getAll");
        return result;
    }

    public PatientDTO getPatientById(Long id) {
        observabilityService.start("service.patient.getById");
        Optional<PatientEntity> patientEntity = patientRepository.findById(id);
        PatientDTO result = patientEntity.map(PatientMapper::toDTO).orElse(null);
        observabilityService.stop("service.patient.getById");
        return result;
    }

    public PatientDTO savePatient(PatientDTO patientDTO) {
        observabilityService.start("service.patient.save");
        PatientEntity entity = PatientMapper.toEntity(patientDTO);
        PatientDTO result = PatientMapper.toDTO(patientRepository.save(entity));
        observabilityService.stop("service.patient.save");
        return result;
    }

    public PatientDTO updatePatient(Long id, PatientDTO newPatientDTO) {
        observabilityService.start("service.patient.update");
        PatientDTO result = patientRepository.findById(id)
                .map(existingPatient -> {
                    existingPatient.setFio(newPatientDTO.getFio());
                    existingPatient.setDateOfBirth(newPatientDTO.getDateOfBirth());
                    existingPatient.setInsuranceNumber(newPatientDTO.getInsuranceNumber());
                    return PatientMapper.toDTO(patientRepository.save(existingPatient));
                })
                .orElse(null);
        observabilityService.stop("service.patient.update");
        return result;
    }

    public void deletePatient(Long id) {
        observabilityService.start("service.patient.delete");
        patientRepository.deleteById(id);
        observabilityService.stop("service.patient.delete");
    }
}
