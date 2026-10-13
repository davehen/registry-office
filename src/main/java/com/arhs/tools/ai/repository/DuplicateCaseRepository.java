package com.arhs.tools.ai.repository;
import com.arhs.tools.ai.domain.DuplicateCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface DuplicateCaseRepository extends JpaRepository<DuplicateCase, Long>, JpaSpecificationExecutor<DuplicateCase> { }
