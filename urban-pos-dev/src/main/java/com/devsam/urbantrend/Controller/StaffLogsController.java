package com.devsam.urbantrend.Controller;

import com.devsam.urbantrend.Dtos.StaffLogResponse;
import com.devsam.urbantrend.Entity.StaffLog;
import com.devsam.urbantrend.Entity.User;
import com.devsam.urbantrend.Repository.StaffLogRepository;
import com.devsam.urbantrend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/staff-logs")
@RequiredArgsConstructor
public class StaffLogsController {

    private final StaffLogRepository repository;
    private final UserRepository userRepository; // 👈 swapped from staffRepository

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<StaffLogResponse> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @RequestParam(required = false) String staffId
    ) {
        List<StaffLog> logs;

        if (from != null && to != null) {
            logs = repository.findByTimestampBetween(from, to);
        } else if (staffId != null) {
            logs = repository.findByStaffId(staffId);
        } else {
            logs = repository.findAll();
        }

        return toResponseList(logs);
    }

    private List<StaffLogResponse> toResponseList(List<StaffLog> logs) {

        List<UUID> userIds = logs.stream()
                .map(log -> tryParseUuid(log.getStaffId()))
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> userNames = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(
                        u -> u.getId().toString(),
                        User::getFirstName
                ));

        return logs.stream()
                .map(log -> new StaffLogResponse(
                        log.getId().toString(),
                        log.getStaffId(),
                        resolveStaffName(log.getStaffId(), userNames),
                        log.getAction(),
                        log.getEntity(),
                        log.getEntityId(),
                        log.getEndpoint(),
                        log.getMethod(),
                        log.getTimestamp(),
                        log.getDescription()
                ))
                .collect(Collectors.toList());
    }

    private UUID tryParseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null; // not a real UUID (e.g. "SYSTEM")
        }
    }

    private String resolveStaffName(String staffId, Map<String, String> userNames) {
        UUID parsed = tryParseUuid(staffId);
        if (parsed == null) {
            return staffId; // e.g. "SYSTEM" — display as-is
        }
        return userNames.getOrDefault(staffId, "Unknown");
    }
}