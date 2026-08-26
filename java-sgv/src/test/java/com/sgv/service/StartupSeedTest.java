package com.sgv.service;

import com.sgv.config.DataInitializer;
import com.sgv.entity.Sale;
import com.sgv.entity.User;
import com.sgv.repository.SaleRepository;
import com.sgv.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.11 — Arranque/seed (DataInitializer): seeds seguros e idempotentes.
 *
 * Estado actual (antes das correções da auditoria):
 *  - demoSale_isMarkedAsDemo: FALHA (BUG-006 — a venda de demonstração nº 1
 *    é criada sem demoFlag → conta como venda real em todos os relatórios).
 *  - deactivatedAdmin_staysDeactivatedAfterRestart: FALHA (BUG-006 —
 *    ensureUser faz setActive(true) incondicional sobre o utilizador já
 *    existente: desactivar o admin é revertido no arranque seguinte).
 *  - adminNotReseededWhenUsersExist: FALHA (BUG-006 — o admin é (re)criado
 *    com password default em qualquer arranque, mesmo com BD com utilizadores).
 *  - existingAdmin_passwordNotOverwritten: PASSA (o hash existente não é
 *    sobrescrito — mantém).
 *
 * Nota: o DataInitializer corre uma vez no arranque do contexto de teste
 * (CommandLineRunner); estes testes invocam run() de novo para simular o
 * arranque seguinte. A classe não é @Transactional: os efeitos do run()
 * commitam (como num arranque real).
 */
@SpringBootTest
@ActiveProfiles("test")
class StartupSeedTest {

    @Autowired private DataInitializer dataInitializer;
    @Autowired private UserRepository userRepository;
    @Autowired private SaleRepository saleRepository;

    @Test
    void demoSale_isMarkedAsDemo() {
        // BUG-006 — FALHA AGORA: sale nº 1 (Consumidor Final) sem demoFlag.
        Sale demo = saleRepository.findAll().stream()
                .filter(s -> "Consumidor Final".equals(s.getCustomerName()))
                .filter(s -> "VENDA".equals(s.getDocumentType()))
                .filter(s -> s.getDocumentNumber() != null && s.getDocumentNumber() == 1L)
                .findFirst()
                .orElseThrow(() -> new AssertionError("venda demo do seed não encontrada"));

        assertTrue(Boolean.TRUE.equals(demo.getDemoFlag()),
                "BUG-006: a venda de demonstração do seed deve ter demoFlag=true "
                        + "(hoje entra em todos os relatórios como venda real)");
    }

    @Test
    void deactivatedAdmin_staysDeactivatedAfterRestart() throws Exception {
        // BUG-006 — FALHA AGORA: arrancar reactiva o utilizador desactivado.
        User admin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("utilizador admin do seed não encontrado"));

        admin.setActive(false);
        userRepository.save(admin);

        dataInitializer.run(); // simula o arranque seguinte

        User after = userRepository.findByUsername("admin").orElseThrow();
        assertFalse(after.isActive(),
                "BUG-006: um utilizador desactivado tem de PERMANECER desactivado "
                        + "após o arranque — a revogação de acesso é ineficaz");
    }

    @Test
    void adminNotReseededWhenUsersExist() throws Exception {
        // BUG-006 — FALHA AGORA: o admin é (re)criado com password default em
        // qualquer arranque, mesmo com a BD a ter já utilizadores.
        // Premissa garantida: um utilizador de teste (commitado — a classe é
        // não-transaccional) assegura que a BD nunca fica vazia.
        User other = new User();
        other.setUsername("seed-checker-" + System.nanoTime());
        other.setPasswordHash("x");
        userRepository.save(other);

        User admin = userRepository.findByUsername("admin").orElseThrow();
        long usersBefore = userRepository.count();
        assertTrue(usersBefore > 1, "premissa: a BD deve ter mais utilizadores além do admin");

        userRepository.delete(admin);
        dataInitializer.run(); // simula o arranque seguinte

        assertTrue(userRepository.findByUsername("admin").isEmpty(),
                "BUG-006: seeds de utilizadores devem correr só em BD vazia — "
                        + "com utilizadores existentes, o admin não deve ser (re)criado "
                        + "com password default");
    }

    @Test
    void existingAdmin_passwordNotOverwritten() throws Exception {
        // Comportamento CORRECTO a manter: hash de password existente não é
        // tocado pelo seed.
        User admin = userRepository.findByUsername("admin").orElseThrow();
        String hashBefore = admin.getPasswordHash();
        assertFalse(hashBefore == null || hashBefore.isBlank());

        dataInitializer.run();

        User after = userRepository.findByUsername("admin").orElseThrow();
        assertEquals(hashBefore, after.getPasswordHash(),
                "o seed não pode sobrescrever a password existente do admin");
    }
}
