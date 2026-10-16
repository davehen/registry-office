package com.arhs.tools.ai.repository;
import com.arhs.tools.ai.domain.DuplicateCaseEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DuplicateCaseEventRepository extends JpaRepository<DuplicateCaseEvent, Long> {
    List<DuplicateCaseEvent> findByCaseIdOrderByOccurredAtAscIdAsc(Long caseId);
}
