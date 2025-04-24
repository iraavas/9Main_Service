package ru.hpclab.hl.module1.service.statistics;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import ru.hpclab.hl.module1.model.statistics.Timing;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class ObservabilityService {
    private static final Instant PENDING_STOP = null;

    private final List<Integer> intervals;
    private final int delay;

    private final Set<Timing> timings = new ConcurrentSkipListSet<>(Comparator.comparing(Timing::getStart));

    public ObservabilityService(List<Integer> intervals, int delay) {
        this.intervals = intervals;
        this.delay = delay;
    }

    public void start(String name) {
        Timing timing = new Timing(name, Instant.now(), PENDING_STOP);
        timings.add(timing);
    }

    public void stop(String name) {
        Instant stopTime = Instant.now();
        Optional<Timing> timingOpt = timings.stream()
                .filter(t -> t.getName().equals(name) && t.getStop() == PENDING_STOP)
                .findFirst();

        timingOpt.ifPresent(t -> t.setStop(stopTime));
    }

    private void removeOldTimings(Instant now, int maxInterval) {
        timings.removeIf(t -> now.minusSeconds(maxInterval).isAfter(t.getStart()));
    }

    @Async(value = "applicationTaskExecutor")
    @Scheduled(fixedDelayString = "${service.statistic.observability.delay}")
    public void getStatistics() {
        List<Timing> snapshot = new ArrayList<>(timings);
        Instant now = Instant.now();
        int maxInterval = intervals.stream().max(Integer::compare).orElse(60);

        removeOldTimings(now, maxInterval);

        Set<String> uniqueNames = snapshot.stream()
                .map(Timing::getName)
                .collect(Collectors.toSet());

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        for (String name : uniqueNames) {
            for (int interval : intervals) {
                Instant threshold = now.minusSeconds(interval);

                List<Timing> filtered = snapshot.stream()
                        .filter(t -> t.getStop() != null && t.getStart().isAfter(threshold) && t.getName().equals(name))
                        .toList();

                if (!filtered.isEmpty()) {
                    double avg = filtered.stream()
                            .mapToLong(t -> Duration.between(t.getStart(), t.getStop()).toMillis())
                            .average().orElse(0);

                    System.out.printf("[%s] %ds: %s — %.3f s%n", timestamp, interval, name, avg / 1000);
                }
            }
        }
    }
}
