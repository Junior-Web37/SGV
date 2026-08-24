package com.sgv.service;

import com.sgv.entity.CashMovement;
import com.sgv.entity.CashSession;
import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.CashMovementRepository;
import com.sgv.repository.CashSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashSessionServiceTest {

    @Mock
    private CashSessionRepository cashSessionRepository;

    @Mock
    private CashMovementRepository cashMovementRepository;

    private User userWithPermission(String permission) {
        Role role = new Role();
        role.setName("TEST");
        role.setPermissions(Set.of(permission));
        User user = new User();
        user.setUsername("testuser");
        user.setRoles(Set.of(role));
        return user;
    }

    @Test
    void requireOpenToday_nullUser_returnsSessionError() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        String result = service.requireOpenToday(null);
        assertNotEquals(CashSessionService.OK, result);
        assertTrue(result.contains("login"));
    }

    @Test
    void requireOpenToday_noOpenSession_returnsError() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        User user = userWithPermission("CAIXA:VIEW");
        when(cashSessionRepository.findByUserAndState(user, "OPEN")).thenReturn(Optional.empty());

        String result = service.requireOpenToday(user);

        assertNotEquals(CashSessionService.OK, result);
        assertTrue(result.contains("turno"));
    }

    @Test
    void requireOpenToday_openToday_returnsOk() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        User user = userWithPermission("CAIXA:VIEW");
        CashSession session = new CashSession();
        session.setOpenedAt(LocalDateTime.now());
        when(cashSessionRepository.findByUserAndState(user, "OPEN")).thenReturn(Optional.of(session));

        assertEquals(CashSessionService.OK, service.requireOpenToday(user));
    }

    @Test
    void requireOpenToday_staleSessionFromPreviousDay_returnsError() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        User user = userWithPermission("CAIXA:VIEW");
        CashSession session = new CashSession();
        session.setOpenedAt(LocalDate.now().minusDays(1).atTime(9, 0));
        when(cashSessionRepository.findByUserAndState(user, "OPEN")).thenReturn(Optional.of(session));

        String result = service.requireOpenToday(user);

        assertNotEquals(CashSessionService.OK, result);
        assertTrue(result.contains("ainda não foi fechado"));
    }

    @Test
    void openSession_whenAlreadyOpen_throws() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        User user = userWithPermission("CAIXA:VIEW");
        when(cashSessionRepository.existsByUserAndState(user, "OPEN")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.openSession(user, new BigDecimal("50.00")));
    }

    @Test
    void openSession_withoutPermission_throws() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        User user = userWithPermission("PRODUTOS:VIEW");

        assertThrows(IllegalStateException.class,
                () -> service.openSession(user, new BigDecimal("50.00")));
        verify(cashSessionRepository, never()).save(any());
    }

    @Test
    void openSession_success_savesOpenSession() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        User user = userWithPermission("CAIXA:VIEW");
        when(cashSessionRepository.existsByUserAndState(user, "OPEN")).thenReturn(false);
        when(cashSessionRepository.save(any(CashSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CashSession saved = service.openSession(user, new BigDecimal("250.00"));

        assertNotNull(saved);
        assertEquals("OPEN", saved.getState());
        assertEquals(new BigDecimal("250.00"), saved.getInitialValue());
        assertNotNull(saved.getOpenedAt());
    }

    @Test
    void closeSession_calculatesSystemValueFromMovements() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        User user = userWithPermission("CAIXA:VIEW");

        CashSession session = new CashSession();
        session.setInitialValue(new BigDecimal("100.00"));
        session.setState("OPEN");

        CashMovement inflow = new CashMovement();
        inflow.setType("IN");
        inflow.setAmount(new BigDecimal("50.00"));

        CashMovement outflow = new CashMovement();
        outflow.setType("OUT");
        outflow.setAmount(new BigDecimal("20.00"));

        when(cashSessionRepository.findByUserAndState(user, "OPEN")).thenReturn(Optional.of(session));
        when(cashMovementRepository.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of(inflow, outflow));
        when(cashSessionRepository.save(any(CashSession.class))).thenAnswer(inv -> inv.getArgument(0));

        CashSession closed = service.closeSession(user, new BigDecimal("130.00"), "fecho de teste");

        assertEquals("CLOSED", closed.getState());
        assertEquals(0, new BigDecimal("130.00").compareTo(closed.getSystemValue()));
        assertEquals(new BigDecimal("130.00"), closed.getReportedValue());
    }

    @Test
    void closeSession_withoutOpenSession_throws() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        User user = userWithPermission("CAIXA:VIEW");
        when(cashSessionRepository.findByUserAndState(user, "OPEN")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.closeSession(user, new BigDecimal("0.00"), null));
    }

    @Test
    void registerMovement_withoutPermission_throws() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        User user = userWithPermission("PRODUTOS:VIEW");

        assertThrows(IllegalStateException.class,
                () -> service.registerMovement(user, "IN", new BigDecimal("10.00"), "desc"));
    }

    @Test
    void hasOpenSession_delegatesToRepository() {
        CashSessionService service = new CashSessionService(cashSessionRepository, cashMovementRepository);
        User user = userWithPermission("CAIXA:VIEW");
        when(cashSessionRepository.existsByUserAndState(user, "OPEN")).thenReturn(true);

        assertTrue(service.hasOpenSession(user));
    }
}
