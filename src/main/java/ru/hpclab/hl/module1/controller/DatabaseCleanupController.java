package ru.hpclab.hl.module1.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.hpclab.hl.module1.service.DatabaseCleanupService;
import ru.hpclab.hl.module1.service.statistics.ObservabilityService;

@RestController
@RequestMapping("/clear")
public class DatabaseCleanupController {

    private final DatabaseCleanupService cleanupService;
    private final ObservabilityService observabilityService;

    public DatabaseCleanupController(DatabaseCleanupService cleanupService, ObservabilityService observabilityService) {
        this.cleanupService = cleanupService;
        this.observabilityService = observabilityService;
    }

    @DeleteMapping
    public void clearDatabase() {
        observabilityService.start("controller.cleanup.clear");
        cleanupService.clear();
        observabilityService.stop("controller.cleanup.clear");
    }
}
