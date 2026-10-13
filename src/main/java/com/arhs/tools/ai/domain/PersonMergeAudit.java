package com.arhs.tools.ai.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.Instant;

@Entity @Immutable
public class PersonMergeAudit {
    @Id @GeneratedValue private Long id;
    @Column(nullable = false, updatable = false) private Long caseId;
    @Column(nullable = false, updatable = false) private int canonicalPersonId;
    @Column(nullable = false, updatable = false) private int retiredPersonId;
    @Column(nullable = false, updatable = false) private Instant mergedAt;
    @Column(length = 500, nullable = false, updatable = false) private String reason;
    @Column(length = 100, updatable = false) private String operatorName;
    // Scalar IDs rather than cascading relations preserve history after deletion.
    @Lob @Column(nullable = false, updatable = false) private String evidenceJson;
    protected PersonMergeAudit() { }
    public PersonMergeAudit(Long caseId, int canonical, int retired, Instant at,
                            String reason, String operator, String evidence) {
        this.caseId = caseId; canonicalPersonId = canonical; retiredPersonId = retired;
        mergedAt = at; this.reason = reason; operatorName = operator; evidenceJson = evidence;
    }
    public Long getId() { return id; }
    public Long getCaseId() { return caseId; }
    public int getCanonicalPersonId() { return canonicalPersonId; }
    public int getRetiredPersonId() { return retiredPersonId; }
    public Instant getMergedAt() { return mergedAt; }
    public String getReason() { return reason; }
    public String getOperatorName() { return operatorName; }
    public String getEvidenceJson() { return evidenceJson; }
}
