package ru.hpclab.hl.module1.controller;

import org.springframework.web.bind.annotation.*;
import ru.hpclab.hl.module1.dto.AppointmentDTO;
import ru.hpclab.hl.module1.service.AppointmentService;
import ru.hpclab.hl.module1.service.statistics.ObservabilityService;

import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final ObservabilityService observabilityService;

    public AppointmentController(AppointmentService appointmentService, ObservabilityService observabilityService) {
        this.appointmentService = appointmentService;
        this.observabilityService = observabilityService;
    }

    @GetMapping
    public List<AppointmentDTO> getAllAppointments() {
        observabilityService.start("controller.appointments.getAll");
        List<AppointmentDTO> result = appointmentService.getAllAppointments();
        observabilityService.stop("controller.appointments.getAll");
        return result;
    }

    @GetMapping("/{id}")
    public AppointmentDTO getAppointmentById(@PathVariable Long id) {
        observabilityService.start("controller.appointments.getById");
        AppointmentDTO result = appointmentService.getAppointmentById(id);
        observabilityService.stop("controller.appointments.getById");
        return result;
    }

    @PostMapping
    public AppointmentDTO addAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        observabilityService.start("controller.appointments.add");
        AppointmentDTO result = appointmentService.saveAppointment(appointmentDTO);
        observabilityService.stop("controller.appointments.add");
        return result;
    }

    @DeleteMapping("/{id}")
    public void deleteAppointment(@PathVariable Long id) {
        observabilityService.start("controller.appointments.delete");
        appointmentService.deleteAppointment(id);
        observabilityService.stop("controller.appointments.delete");
    }

    @PutMapping("/{id}")
    public AppointmentDTO updateAppointment(@PathVariable Long id, @RequestBody AppointmentDTO appointmentDTO) {
        observabilityService.start("controller.appointments.update");
        AppointmentDTO result = appointmentService.updateAppointment(id, appointmentDTO);
        observabilityService.stop("controller.appointments.update");
        return result;
    }
}
