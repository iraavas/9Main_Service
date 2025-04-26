package ru.hpclab.hl.module1.service;

import org.springframework.stereotype.Service;
import ru.hpclab.hl.module1.dto.DoctorDTO;
import ru.hpclab.hl.module1.entity.DoctorEntity;
import ru.hpclab.hl.module1.mapper.DoctorMapper;
import ru.hpclab.hl.module1.repository.AppointmentRepository;
import ru.hpclab.hl.module1.repository.DoctorRepository;
import ru.hpclab.hl.module1.service.statistics.ObservabilityService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final ObservabilityService observabilityService;

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         ObservabilityService observabilityService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.observabilityService = observabilityService;
    }

    public List<DoctorDTO> getAllDoctors() {
        observabilityService.start("service.doctor.getAll");
        List<DoctorDTO> result = doctorRepository.findAll().stream()
                .map(DoctorMapper::toDTO)
                .collect(Collectors.toList());
        observabilityService.stop("service.doctor.getAll");
        return result;
    }

    public DoctorDTO getDoctorById(Long id) {
        observabilityService.start("service.doctor.getById");
        Optional<DoctorEntity> doctorEntity = doctorRepository.findById(id);
        DoctorDTO result = doctorEntity.map(DoctorMapper::toDTO).orElse(null);
        observabilityService.stop("service.doctor.getById");
        return result;
    }

    public DoctorDTO saveDoctor(DoctorDTO doctorDTO) {
        observabilityService.start("service.doctor.save");
        DoctorEntity entity = DoctorMapper.toEntity(doctorDTO);
        DoctorDTO result = DoctorMapper.toDTO(doctorRepository.save(entity));
        observabilityService.stop("service.doctor.save");
        return result;
    }

    public DoctorDTO updateDoctor(Long id, DoctorDTO newDoctorDTO) {
        observabilityService.start("service.doctor.update");
        DoctorDTO result = doctorRepository.findById(id)
                .map(existingDoctor -> {
                    existingDoctor.setFio(newDoctorDTO.getFio());
                    existingDoctor.setSpecialization(newDoctorDTO.getSpecialization());
                    existingDoctor.setWorkSchedule(newDoctorDTO.getWorkSchedule());
                    return DoctorMapper.toDTO(doctorRepository.save(existingDoctor));
                })
                .orElse(null);
        observabilityService.stop("service.doctor.update");
        return result;
    }

    public void deleteDoctor(Long id) {
        observabilityService.start("service.doctor.delete");
        doctorRepository.deleteById(id);
        observabilityService.stop("service.doctor.delete");
    }

}
