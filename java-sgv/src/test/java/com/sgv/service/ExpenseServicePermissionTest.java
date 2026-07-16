package com.sgv.service;

import com.sgv.dto.ExpenseRequest;
import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.BranchRepository;
import com.sgv.repository.ExpenseRepository;
import com.sgv.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExpenseServicePermissionTest {

    private ExpenseRepository expenseRepository;
    private UserRepository userRepository;
    private BranchRepository branchRepository;
    private AuditLogService auditLogService;
    private ExpenseService service;

    @BeforeEach
    void setUp() throws Exception {
        expenseRepository = mock(ExpenseRepository.class);
        userRepository = mock(UserRepository.class);
        branchRepository = mock(BranchRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new ExpenseService();
        setPrivateField(service, "expenseRepository", expenseRepository);
        setPrivateField(service, "userRepository", userRepository);
        setPrivateField(service, "branchRepository", branchRepository);
        setPrivateField(service, "auditLogService", auditLogService);
    }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void createExpense_shouldRejectWithoutFinanceiroPermission() {
        ExpenseRequest request = new ExpenseRequest();
        request.setDescription("Compra de materiais");
        request.setAmount(100.0);
        request.setBranchId(1L);
        request.setPaid(false);

        User user = new User();
        Role role = new Role();
        role.setName("CAIXA");
        role.setPermissions(Set.of("VENDAS:VIEW"));
        user.setRoles(Set.of(role));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalStateException.class, () -> service.createExpense(request, 1L));
    }
}
