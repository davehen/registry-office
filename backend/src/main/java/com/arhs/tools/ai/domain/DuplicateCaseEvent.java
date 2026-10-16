package com.arhs.tools.ai.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

@Entity @Immutable
public class DuplicateCaseEvent {
    @Id @GeneratedValue private Long id;
    @Column(nullable = false, updatable = false) private Long caseId;
    @Column(nullable = false, updatable = false) private String type;
    @Column(nullable = false, updatable = false) private Instant occurredAt;
    @Column(length = 500, updatable = false) private String reason;
    @Column(length = 100, updatable = false) private String operatorName;
    protected DuplicateCaseEvent() { }
    public DuplicateCaseEvent(Long caseId, String type, Instant at, String reason, String operator) {
        this.caseId = caseId; this.type = type; occurredAt = at;
        this.reason = reason; operatorName = operator;
    }
    public Long getId() { return id; }
    public String getType() { return type; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getReason() { return reason; }
    public String getOperatorName() { return operatorName; }
}
