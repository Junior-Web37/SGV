package com.sgv.repository;

import com.sgv.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Customer> searchByCodeOrName(@Param("search") String search);
    
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.type = :type")
    List<Customer> searchByCodeOrNameAndType(@Param("search") String search, @Param("type") String type);
    
    @Query("SELECT c FROM Customer c WHERE c.type = :type")
    List<Customer> findByType(@Param("type") String type);
}
