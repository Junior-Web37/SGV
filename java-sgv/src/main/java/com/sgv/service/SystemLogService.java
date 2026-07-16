package com.sgv.service;

import com.sgv.entity.AuditLog;
import com.sgv.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;

@Service
public class SystemLogService {

    private final AuditLogRepository auditLogRepository;

    public SystemLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Regista um erro do sistema (exceções de try-catch).
     */
    public void logError(String action, String details, Throwable ex) {
        log("ERROR", "SYSTEM", action, details, ex);
    }

    /**
     * Regista uma ação feita por um utilizador (ex: Criou venda, Editou cliente).
     */
    public void logUserAction(String username, String action, String details) {
        log("USER_ACTION", username, action, details, null);
    }

    /**
     * Regista um evento de segurança (ex: Login falhado, tentativa de acesso não autorizado).
     */
    public void logSecurity(String username, String action, String details) {
        log("SECURITY", username, action, details, null);
    }

    /**
     * Regista um evento geral de sistema (ex: Backup iniciado, Configuração alterada).
     */
    public void logSystem(String action, String details) {
        log("SYSTEM", "SYSTEM", action, details, null);
    }

    private void log(String category, String username, String action, String details, Throwable ex) {
        try {
            AuditLog log = new AuditLog();
            log.setCategory(category);
            log.setUsername(username != null ? username : "UNKNOWN");
            log.setAction(action);
            log.setDetails(details);

            if (ex != null) {
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                log.setStackTrace(sw.toString());
            }

            auditLogRepository.save(log);
        } catch (Exception internalEx) {
            System.err.println("CRITICAL: Failed to write to audit log!");
            internalEx.printStackTrace();
        }
    }
}
