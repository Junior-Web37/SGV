package com.sgv.service;

import com.sgv.entity.Customer;
import com.sgv.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SystemLogService systemLogService;

    public CustomerService(CustomerRepository customerRepository, SystemLogService systemLogService) {
        this.customerRepository = customerRepository;
        this.systemLogService = systemLogService;
    }

    public Optional<Customer> findByCodeIgnoreCase(String code) {
        return customerRepository.findByCodeIgnoreCase(code);
    }

    public java.util.List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public boolean existsByCode(String code, Long excludeId) {
        return customerRepository.findByCodeIgnoreCase(code)
                .filter(c -> excludeId == null || !excludeId.equals(c.getId()))
                .isPresent();
    }

    public String generateNewCode() {
        return Customer.generateCode(customerRepository);
    }

    @Transactional
    public Customer saveCustomer(Customer c) {
        if (c == null) throw new IllegalArgumentException("Customer is null");
        if (c.getCreatedAt() == null) c.setCreatedAt(java.time.LocalDateTime.now());
        return customerRepository.save(c);
    }

    @Transactional
    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }
}
