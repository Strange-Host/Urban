package com.devsam.urbantrend.Service.Auth;

import com.devsam.urbantrend.Entity.StaffLog;
import com.devsam.urbantrend.Repository.StaffLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StaffLogService {

    private final StaffLogRepository repository;

    public void log(String staffId,
                    String action,
                    String entity,
                    String entityId,
                    String endpoint,
                    String method,
                    String description) {

        StaffLog log = new StaffLog();
        log.setStaffId(staffId);
        log.setAction(action);
        log.setEntity(entity);
        log.setEntityId(entityId);
        log.setEndpoint(endpoint);
        log.setMethod(method);
        log.setTimestamp(LocalDateTime.now());
        log.setDescription(description);

        repository.save(log);
    }
}
