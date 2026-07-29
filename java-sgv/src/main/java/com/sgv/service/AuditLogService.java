package com.sgv.service;

import com.sgv.entity.AuditLog;
import com.sgv.repository.AuditLogRepository;
import com.sgv.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    public void log(Long userId, String action, String tableName, Long targetId, String details) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setAction(action);
            
            if (userId != null) {
                userRepository.findById(userId).ifPresent(user -> logEntry.setUsername(user.getUsername()));
            }
            
            logEntry.setDetails(tableName + ":" + targetId + " - " + details);
            logEntry.setCreatedAt(LocalDateTime.now());
            
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Erro ao registar audit log: {}", e.getMessage(), e);
        }
    }

    public void log(String action, String tableName, Long targetId, String details) {
        log(null, action, tableName, targetId, details);
    }
}
