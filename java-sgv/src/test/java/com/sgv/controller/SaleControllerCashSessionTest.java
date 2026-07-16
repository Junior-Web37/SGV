package com.sgv.controller;

import com.sgv.repository.AuditLogRepository;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.CustomerRepository;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.SaleRepository;
import com.sgv.repository.StockMovementRepository;
import com.sgv.repository.UserRepository;
import com.sgv.service.AppConfigService;
import com.sgv.service.CashSessionService;
import com.sgv.service.SaleNumberingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaleControllerCashSessionTest {

    private SaleController controller;
    private CashSessionService cashSessionService;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        SaleRepository saleRepository = mock(SaleRepository.class);
        BranchRepository branchRepository = mock(BranchRepository.class);
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StockMovementRepository stockMovementRepository = mock(StockMovementRepository.class);
        userRepository = mock(UserRepository.class);
        SaleNumberingService saleNumberingService = mock(SaleNumberingService.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        AppConfigService appConfigService = mock(AppConfigService.class);
        cashSessionService = mock(CashSessionService.class);

        controller = new SaleController(
                saleRepository,
                branchRepository,
                customerRepository,
                productRepository,
                stockMovementRepository,
                userRepository,
                saleNumberingService,
                auditLogRepository,
                appConfigService,
                cashSessionService
        );
    }

    @Test
    @DisplayName("Utilizador sem sessão válida deve bloquear a criação de venda")
    void nullUserIsBlockedBeforeCreatingSale() {
        ResponseEntity<?> response = controller.validateCashSessionForSale(null);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Caixa fechado", ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    @DisplayName("Sessão aberta hoje deve permitir a criação da venda")
    void openSessionTodayAllowsSaleCreation() {
        com.sgv.entity.User user = new com.sgv.entity.User();
        user.setId(1L);
        user.setUsername("caixa1");
        when(cashSessionService.requireOpenToday(user)).thenReturn(CashSessionService.OK);

        ResponseEntity<?> response = controller.validateCashSessionForSale(user);

        assertNull(response);
    }

    @Test
    @DisplayName("Estado de caixa fechado deve ser reportado ao frontend")
    void cashSessionStatusReportsClosedWhenNoOpenSession() {
        com.sgv.entity.User user = new com.sgv.entity.User();
        user.setId(2L);
        user.setUsername("caixa2");
        when(userRepository.findByUsername("caixa2")).thenReturn(java.util.Optional.of(user));
        when(cashSessionService.requireOpenToday(user)).thenReturn("Não existe turno de caixa aberto. Abra o caixa antes de iniciar uma venda.");

        Principal principal = () -> "caixa2";
        ResponseEntity<?> response = controller.getCashSessionStatus(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(false, body.get("ok"));
        assertEquals("Não existe turno de caixa aberto. Abra o caixa antes de iniciar uma venda.", body.get("message"));
    }
}
