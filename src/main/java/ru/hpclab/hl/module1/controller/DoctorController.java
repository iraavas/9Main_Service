package ru.hpclab.hl.module1.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.hpclab.hl.module1.dto.DoctorDTO;
import ru.hpclab.hl.module1.service.DoctorService;
import ru.hpclab.hl.module1.service.statistics.ObservabilityService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final ObservabilityService observabilityService;

    public DoctorController(DoctorService doctorService, ObservabilityService observabilityService) {
        this.doctorService = doctorService;
        this.observabilityService = observabilityService;
    }

    @GetMapping
    public List<DoctorDTO> getAllDoctors() {
        observabilityService.start("controller.doctors.getAll");
        List<DoctorDTO> result = doctorService.getAllDoctors();
        observabilityService.stop("controller.doctors.getAll");
        return result;
    }

    @GetMapping("/{id}")
    public DoctorDTO getDoctorById(@PathVariable Long id) {
        observabilityService.start("controller.doctors.getById");
        DoctorDTO result = doctorService.getDoctorById(id);
        observabilityService.stop("controller.doctors.getById");
        return result;
    }

    @PostMapping
    public DoctorDTO addDoctor(@RequestBody DoctorDTO doctorDTO) {
        observabilityService.start("controller.doctors.add");
        DoctorDTO result = doctorService.saveDoctor(doctorDTO);
        observabilityService.stop("controller.doctors.add");
        return result;
    }

    @PutMapping("/{id}")
    public DoctorDTO updateDoctor(@PathVariable Long id, @RequestBody DoctorDTO doctorDTO) {
        observabilityService.start("controller.doctors.update");
        DoctorDTO result = doctorService.updateDoctor(id, doctorDTO);
        observabilityService.stop("controller.doctors.update");
        return result;
    }

    @DeleteMapping("/{id}")
    public void deleteDoctor(@PathVariable Long id) {
        observabilityService.start("controller.doctors.delete");
        doctorService.deleteDoctor(id);
        observabilityService.stop("controller.doctors.delete");
    }
}
