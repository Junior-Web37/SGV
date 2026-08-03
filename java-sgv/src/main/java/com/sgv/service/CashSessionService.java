package com.sgv.service;

import com.sgv.entity.CashMovement;
import com.sgv.entity.CashSession;
import com.sgv.entity.User;
import com.sgv.repository.CashMovementRepository;
import com.sgv.repository.CashSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CashSessionService {

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
        requirePermission(user, "CAIXA", "VIEW");
        CashSession session = cashSessionRepository.findByUserAndState(user, "OPEN")
                .orElseThrow(() -> new IllegalStateException("Não existe turno aberto para registar movimentos."));

        CashMovement movement = new CashMovement();
        movement.setSession(session);
        movement.setType(type); // IN or OUT
        movement.setAmount(amount);
        movement.setDescription(description);
        movement.setCreatedAt(LocalDateTime.now());
        movement.setCreatedBy(user);
        return cashMovementRepository.save(movement);
    }

    /**
     * Lista todos os movimentos de um turno.
     */
    public List<CashMovement> getMovements(CashSession session) {
        return cashMovementRepository.findBySessionOrderByCreatedAtAsc(session);
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
