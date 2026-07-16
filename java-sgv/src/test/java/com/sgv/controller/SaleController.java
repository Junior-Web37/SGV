package com.sgv.controller;

import com.sgv.repository.*;
import com.sgv.service.AppConfigService;
import com.sgv.service.CashSessionService;
import com.sgv.service.SaleNumberingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Map;

public class SaleController {

    private final CashSessionService cashSessionService;
    private final com.sgv.repository.UserRepository userRepository;

    public SaleController(SaleRepository saleRepository,
                          BranchRepository branchRepository,
                          CustomerRepository customerRepository,
                          ProductRepository productRepository,
                          StockMovementRepository stockMovementRepository,
                          com.sgv.repository.UserRepository userRepository,
                          SaleNumberingService saleNumberingService,
                          AuditLogRepository auditLogRepository,
                          AppConfigService appConfigService,
                          CashSessionService cashSessionService) {
        this.cashSessionService = cashSessionService;
        this.userRepository = userRepository;
    }

    public ResponseEntity<?> validateCashSessionForSale(com.sgv.entity.User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Caixa fechado"));
        }
        String result = cashSessionService.requireOpenToday(user);
        if (!CashSessionService.OK.equals(result)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Caixa fechado"));
        }
        return null;
    }

    public ResponseEntity<?> getCashSessionStatus(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("ok", false, "message", "Usuário inválido"));
        }
        String username = principal.getName();
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("ok", false, "message", "Usuário inválido"));
        }
        com.sgv.entity.User user = userOpt.get();
        String result = cashSessionService.requireOpenToday(user);
        if (CashSessionService.OK.equals(result)) {
            return ResponseEntity.ok(Map.of("ok", true, "message", result));
        }
        return ResponseEntity.ok(Map.of("ok", false, "message", result));
    }
}
