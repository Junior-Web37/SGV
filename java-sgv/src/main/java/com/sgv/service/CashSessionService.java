package com.sgv.service;

import com.sgv.entity.CashMovement;
import com.sgv.entity.CashSession;
import com.sgv.entity.User;
import com.sgv.repository.CashMovementRepository;
import com.sgv.repository.CashSessionRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.sgv.entity.OperationKind;
import java.util.Optional;

@Service
public class CashSessionService {

    private static final Logger log = LoggerFactory.getLogger(CashSessionService.class);

    /** Mensagem devolvida por {@link #requireOpenToday(User)} quando tudo OK. */
    public static final String OK = "OK";

    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;

    public CashSessionService(CashSessionRepository cashSessionRepository,
                               CashMovementRepository cashMovementRepository) {
        this.cashSessionRepository = cashSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
    }

    /**
     * Verifica se o utilizador tem um turno aberto.
     */
    public boolean hasOpenSession(User user) {
        return cashSessionRepository.existsByUserAndState(user, "OPEN");
    }

    /**
     * Obtém o turno aberto do utilizador, se existir.
     */
    public Optional<CashSession> getOpenSession(User user) {
        return cashSessionRepository.findByUserAndState(user, "OPEN");
    }

    /**
     * Verifica se o utilizador pode fazer uma venda:
     *  - Tem de existir um turno aberto
     *  - O turno aberto tem de ter sido aberto HOJE (caso contrário, há um
     *    turno "esquecido" de um dia anterior que tem de ser fechado primeiro)
     *
     * @return {@link #OK} se tudo bem, ou uma mensagem de erro a apresentar
     *         ao utilizador.
     */
    public String requireOpenToday(User user) {
        if (user == null) {
            return "Sessão de utilizador inválida. Faça login novamente.";
        }
        Optional<CashSession> open = getOpenSession(user);
        if (open.isEmpty()) {
            return "Não existe turno de caixa aberto. Abra o caixa antes de iniciar uma venda.";
        }
        CashSession session = open.get();
        LocalDate openedDate = session.getOpenedAt() != null
                ? session.getOpenedAt().toLocalDate()
                : null;
        LocalDate today = LocalDate.now();
        if (openedDate != null && openedDate.isBefore(today)) {
            return "Existe um turno de caixa aberto desde " + openedDate
                    + " que ainda não foi fechado. Feche-o antes de iniciar uma nova venda.";
        }
        return OK;
    }

    /**
     * Verifica se existem sessões abertas de DIAS ANTERIORES por qualquer
     * utilizador — útil para mostrar avisos no dashboard.
     */
    public List<CashSession> findStaleOpenSessions() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return cashSessionRepository.findByStateAndOpenedAtBefore("OPEN", startOfToday);
    }

    public List<CashSession> findSessions(User currentUser) {
        if (currentUser == null) {
            return List.of();
        }
        if (currentUser.isSuperAdmin() || currentUser.hasPermission("CAIXA", "VIEW")) {
            return cashSessionRepository.findAll(Sort.by(Sort.Direction.DESC, "openedAt"));
        }
        return cashSessionRepository.findByUserOrderByOpenedAtDesc(currentUser);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CashSession> findSessionsWithUser(User currentUser) {
        List<CashSession> sessions = findSessions(currentUser);
        // Force initialize session.user to avoid LazyInitializationException in UI
        for (CashSession s : sessions) {
            if (s.getUser() != null) {
                s.getUser().getFullName();
            }
        }
        return sessions;
    }

    /**
     * Abre um novo turno de caixa para o utilizador.
     * Lança exceção se já existir um turno aberto.
     */
    @Transactional
    public CashSession openSession(User user, BigDecimal initialValue) {
        requirePermission(user, "CAIXA", "VIEW");
        if (hasOpenSession(user)) {
            throw new IllegalStateException("Já existe um turno aberto para este utilizador.");
        }
        CashSession session = new CashSession();
        session.setUser(user);
        session.setBranch(user.getBranch());
        session.setInitialValue(initialValue != null ? initialValue : BigDecimal.ZERO);
        session.setOpenedAt(LocalDateTime.now());
        session.setState("OPEN");
        return cashSessionRepository.save(session);
    }

    /**
     * Fecha o turno de caixa aberto do utilizador.
     */
    @Transactional
    public CashSession closeSession(User user, BigDecimal reportedValue, String notes) {
        requirePermission(user, "CAIXA", "VIEW");
        CashSession session = cashSessionRepository.findByUserAndState(user, "OPEN")
                .orElseThrow(() -> new IllegalStateException("Não existe turno aberto para este utilizador."));

        // Calcular o valor do sistema baseado nos movimentos
        BigDecimal systemValue = calculateSystemValue(session);

        session.setClosedAt(LocalDateTime.now());
        session.setReportedValue(reportedValue);
        session.setSystemValue(systemValue);
        session.setNotes(notes);
        session.setState("CLOSED");
        return cashSessionRepository.save(session);
    }

    /**
     * Regista um movimento (entrada ou saída) no turno aberto do utilizador.
     */
    @Transactional
    public CashMovement registerMovement(User user, String type, BigDecimal amount, String description) {
        return registerMovement(user, type, amount, description, null);
    }

    @Transactional
    public CashMovement registerMovement(User user, String type, BigDecimal amount, String description, String reason) {
        return registerMovement(user, type, amount, description, reason, true);
    }

    @Transactional
    public CashMovement registerMovement(User user, String type, BigDecimal amount, String description, String reason, boolean requireCashPermission) {
        return registerMovement(user, type, amount, description, reason, null, requireCashPermission);
    }

    @Transactional
    public CashMovement registerMovement(User user, String type, BigDecimal amount, String description, String reason, OperationKind operationKind, boolean requireCashPermission) {
        log.info("registerMovement called by user={} type={} amount={} description={} reason={} operationKind={} permissionCheck={}",
            user != null ? user.getUsername() : "null", type, amount, description, reason, operationKind, requireCashPermission);
        if (requireCashPermission) {
            requirePermission(user, "CAIXA", "VIEW");
        }
        CashSession session = cashSessionRepository.findByUserAndState(user, "OPEN")
            .orElseThrow(() -> new IllegalStateException("Não existe turno aberto para registar movimentos."));

        CashMovement movement = new CashMovement();
        movement.setSession(session);
        movement.setType(type); // IN or OUT
        movement.setAmount(amount);
        movement.setDescription(description);
        movement.setReason(reason);
        // Use explicit operationKind when provided, otherwise infer
        OperationKind kind = operationKind != null ? operationKind : inferOperationKind(description, reason);
        movement.setOperationKind(kind);
        movement.setCreatedAt(LocalDateTime.now());
        movement.setCreatedBy(user);
        CashMovement saved = cashMovementRepository.save(movement);
        log.info("registerMovement saved id={} sessionId={}", saved != null ? saved.getId() : null,
            saved != null && saved.getSession() != null ? saved.getSession().getId() : null);
        return saved;
    }

    /**
     * Heurística simples para inferir `OperationKind` a partir da descrição/motivo.
     * Pode ser expandida ou substituída por um parâmetro explícito nas chamadas.
     */
    private OperationKind inferOperationKind(String description, String reason) {
        String text = "";
        if (reason != null) text += reason + " ";
        if (description != null) text += description;
        text = text.trim().toLowerCase();
        if (text.contains("venda") || text.matches(".*\\bv-?\\d+.*")) {
            return OperationKind.SALE;
        }
        if (text.contains("cota") || text.contains("cotação") || text.contains("cotacao")) {
            return OperationKind.QUOTE;
        }
        if (text.contains("devol") || text.contains("reembolso") || text.contains("refund")) {
            return OperationKind.REFUND;
        }
        if (text.contains("transfer") || text.contains("transferência") || text.contains("transferencia")) {
            return OperationKind.TRANSFER;
        }
        return OperationKind.OTHER;
    }

    /**
     * Lista todos os movimentos de um turno.
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CashMovement> getMovements(CashSession session) {
        List<CashMovement> movements = cashMovementRepository.findBySessionOrderByCreatedAtAsc(session);
        // Force initialization of lazy associations used by the UI (createdBy)
        // to avoid Hibernate LazyInitializationException when accessed in the JavaFX thread.
        for (CashMovement m : movements) {
            if (m.getCreatedBy() != null) {
                m.getCreatedBy().getFullName();
            }
        }
        return movements;
    }

    /**
     * Calcula o valor do sistema somando o valor inicial + entradas - saídas.
     */
    private void requirePermission(User user, String page, String action) {
        if (user == null || !user.hasPermission(page, action)) {
            throw new IllegalStateException("Não tem permissão para " + page + ":" + action + ".");
        }
    }

    private BigDecimal calculateSystemValue(CashSession session) {
        List<CashMovement> movements = cashMovementRepository.findBySessionOrderByCreatedAtAsc(session);
        BigDecimal total = session.getInitialValue() != null ? session.getInitialValue() : BigDecimal.ZERO;
        for (CashMovement m : movements) {
            if ("IN".equals(m.getType())) {
                total = total.add(m.getAmount());
            } else if ("OUT".equals(m.getType())) {
                total = total.subtract(m.getAmount());
            }
        }
        return total;
    }
}
