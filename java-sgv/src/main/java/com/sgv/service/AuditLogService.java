package com.sgv.service;

import com.sgv.entity.AuditLog;
import com.sgv.repository.AuditLogRepository;
import com.sgv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    public void log(Long userId, String action, String tableName, Long targetId, String details) {
        try {
            AuditLog log = new AuditLog();
            log.setAction(action);
            
            if (userId != null) {
                userRepository.findById(userId).ifPresent(user -> log.setUsername(user.getUsername()));
            }
            
            log.setDetails(tableName + ":" + targetId + " - " + details);
            log.setCreatedAt(LocalDateTime.now());
            
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Não falhar a operação principal se o log falhar
            System.err.println("Erro ao registar audit log: " + e.getMessage());
        }
    }

    public void log(String action, String tableName, Long targetId, String details) {
        log(null, action, tableName, targetId, details);
    }
}
