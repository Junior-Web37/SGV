package com.sgv.service;

import com.sgv.entity.CashSession;
import com.sgv.entity.User;
import com.sgv.repository.CashMovementRepository;
import com.sgv.repository.CashSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testes unitários à regra "não vender sem caixa aberto hoje".
 *
 * Cobre os 4 cenários:
 *  1. Utilizador nulo
 *  2. Sem turno aberto
 *  3. Turno aberto HOJE → OK
 *  4. Turno aberto em dia anterior → bloqueado
 */
class CashSessionServiceRequireOpenTodayTest {

    private CashSessionRepository cashSessionRepository;
    private CashMovementRepository cashMovementRepository;
    private CashSessionService service;

    @BeforeEach
    void setUp() {
        cashSessionRepository = mock(CashSessionRepository.class);
        cashMovementRepository = mock(CashMovementRepository.class);
        service = new CashSessionService(cashSessionRepository, cashMovementRepository);
    }

    @Test
    @DisplayName("Utilizador nulo → erro")
    void nullUserIsBlocked() {
        String result = service.requireOpenToday(null);
        assertNotEquals(CashSessionService.OK, result, "Não deve aceitar user null");
        assertTrue(result.toLowerCase().contains("utilizador") || result.toLowerCase().contains("login"),
            "Mensagem deve indicar problema de login, mas foi: " + result);
    }

    @Test
    @DisplayName("Sem turno aberto → bloqueado com mensagem clara")
    void noOpenSessionIsBlocked() {
        User u = new User();
        u.setId(1L);
        u.setUsername("caixa1");
        when(cashSessionRepository.findByUserAndState(eq(u), eq("OPEN")))
            .thenReturn(Optional.empty());

        String result = service.requireOpenToday(u);
        assertNotEquals(CashSessionService.OK, result);
        assertTrue(result.toLowerCase().contains("turno"),
            "Mensagem deve mencionar 'turno', mas foi: " + result);
    }

    @Test
    @DisplayName("Turno aberto HOJE → OK (venda permitida)")
    void openSessionTodayIsAllowed() {
        User u = new User();
        u.setId(1L);
        u.setUsername("caixa1");

        CashSession session = new CashSession();
        session.setId(42L);
        session.setUser(u);
        session.setOpenedAt(LocalDate.now().atStartOfDay().plusHours(8));
        session.setState("OPEN");
        when(cashSessionRepository.findByUserAndState(eq(u), eq("OPEN")))
            .thenReturn(Optional.of(session));

        String result = service.requireOpenToday(u);
        assertEquals(CashSessionService.OK, result,
            "Caixa aberto hoje deve permitir venda, mas deu: " + result);
    }

    @Test
    @DisplayName("Turno aberto ONTEM (não fechado) → bloqueado")
    void staleSessionIsBlocked() {
        User u = new User();
        u.setId(1L);
        u.setUsername("caixa1");

        // Sessão aberta há 2 dias e ainda OPEN
        LocalDateTime openedAt = LocalDate.now().atStartOfDay().minusDays(2);
        CashSession session = new CashSession();
        session.setId(99L);
        session.setUser(u);
        session.setOpenedAt(openedAt);
        session.setInitialValue(new BigDecimal("100.00"));
        session.setState("OPEN");
        when(cashSessionRepository.findByUserAndState(eq(u), eq("OPEN")))
            .thenReturn(Optional.of(session));

        String result = service.requireOpenToday(u);
        assertNotEquals(CashSessionService.OK, result,
            "Caixa aberto há 2 dias deve BLOQUEAR venda, mas deu: " + result);
        assertTrue(result.contains(openedAt.toLocalDate().toString()),
            "Mensagem deve indicar a data do turno esquecido, mas foi: " + result);
        assertTrue(result.toLowerCase().contains("fech"),
            "Mensagem deve pedir para fechar, mas foi: " + result);
    }

    @Test
    @DisplayName("Sessão sem openedAt não deve rebentar (null-safe)")
    void sessionWithoutOpenedAtIsHandled() {
        User u = new User();
        u.setId(1L);
        u.setUsername("caixa1");

        CashSession session = new CashSession();
        session.setId(50L);
        session.setUser(u);
        session.setOpenedAt(null);
        session.setState("OPEN");
        when(cashSessionRepository.findByUserAndState(eq(u), eq("OPEN")))
            .thenReturn(Optional.of(session));

        // Sem data, tratamos como se fosse de hoje (não bloqueia)
        String result = service.requireOpenToday(u);
        assertEquals(CashSessionService.OK, result,
            "Sessão sem openedAt não deve bloquear, mas deu: " + result);
    }
}
