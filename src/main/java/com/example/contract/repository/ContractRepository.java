package com.example.contract.repository;

import com.example.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for performing database operations on {@link Contract} entities.
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
}
