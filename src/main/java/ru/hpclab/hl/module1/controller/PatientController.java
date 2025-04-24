package ru.hpclab.hl.module1.controller;

import org.springframework.web.bind.annotation.*;
import ru.hpclab.hl.module1.dto.PatientDTO;
import ru.hpclab.hl.module1.service.PatientService;
import ru.hpclab.hl.module1.service.statistics.ObservabilityService;

import java.util.List;

@RestController
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;
    private final ObservabilityService observabilityService;

    public PatientController(PatientService patientService, ObservabilityService observabilityService) {
        this.patientService = patientService;
        this.observabilityService = observabilityService;
    }

    @GetMapping
    public List<PatientDTO> getAllPatients() {
        observabilityService.start("controller.patients.getAll");
        List<PatientDTO> result = patientService.getAllPatients();
        observabilityService.stop("controller.patients.getAll");
        return result;
    }

    @GetMapping("/{id}")
    public PatientDTO getPatientById(@PathVariable Long id) {
        observabilityService.start("controller.patients.getById");
        PatientDTO result = patientService.getPatientById(id);
        observabilityService.stop("controller.patients.getById");
        return result;
    }

    @PostMapping
    public PatientDTO addPatient(@RequestBody PatientDTO patientDTO) {
        observabilityService.start("controller.patients.add");
        PatientDTO result = patientService.savePatient(patientDTO);
        observabilityService.stop("controller.patients.add");
        return result;
    }

    @PutMapping("/{id}")
    public PatientDTO updatePatient(@PathVariable Long id, @RequestBody PatientDTO patientDTO) {
        observabilityService.start("controller.patients.update");
        PatientDTO result = patientService.updatePatient(id, patientDTO);
        observabilityService.stop("controller.patients.update");
        return result;
    }

    @DeleteMapping("/{id}")
    public void deletePatient(@PathVariable Long id) {
        observabilityService.start("controller.patients.delete");
        patientService.deletePatient(id);
        observabilityService.stop("controller.patients.delete");
    }
}
