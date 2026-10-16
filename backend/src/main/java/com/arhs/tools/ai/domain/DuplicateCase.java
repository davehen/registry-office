package com.arhs.tools.ai.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"left_person_id", "right_person_id"}))
public class DuplicateCase {
    public enum Status { OPEN, DISMISSED, MERGED, OBSOLETE }
    public enum Confidence { HIGH, MEDIUM }
    @Id @GeneratedValue private Long id;
    @Version private long version;
    @Column(name = "left_person_id", nullable = false, updatable = false) private int leftPersonId;
    @Column(name = "right_person_id", nullable = false, updatable = false) private int rightPersonId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.OPEN;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Confidence confidence;
    private int score;
    private String reasons;
    @Column(length = 64) private String fingerprint;
    @Column(nullable = false, updatable = false) private Instant firstDetectedAt;
    @Column(nullable = false) private Instant lastDetectedAt;
    private Instant resolvedAt;
    @Column(length = 500) private String resolutionReason;
    @Column(length = 100) private String operatorName;
    private Long mergeAuditId;

    protected DuplicateCase() { }
    public DuplicateCase(int left, int right, Instant now) {
        if (left >= right) throw new IllegalArgumentException("Pair IDs must be ordered");
        leftPersonId = left; rightPersonId = right;
        firstDetectedAt = now; lastDetectedAt = now;
    }
    public void detect(int score, List<String> reasons, String fingerprint, Instant now) {
        this.score = score; this.reasons = String.join(",", reasons);
        confidence = score >= 75 ? Confidence.HIGH : Confidence.MEDIUM;
        this.fingerprint = fingerprint; lastDetectedAt = now;
    }
    public void reopen() {
        status = Status.OPEN; resolvedAt = null; resolutionReason = null;
        operatorName = null; mergeAuditId = null;
    }
    public void resolve(Status status, String reason, String operator, Instant now, Long auditId) {
        this.status = status; resolutionReason = reason; operatorName = operator;
        resolvedAt = now; mergeAuditId = auditId;
    }
    public Long getId() { return id; }
    public long getVersion() { return version; }
    public int getLeftPersonId() { return leftPersonId; }
    public int getRightPersonId() { return rightPersonId; }
    public Status getStatus() { return status; }
    public Confidence getConfidence() { return confidence; }
    public int getScore() { return score; }
    public List<String> getReasons() { return reasons == null ? List.of() : List.of(reasons.split(",")); }
    public String getFingerprint() { return fingerprint; }
    public Instant getFirstDetectedAt() { return firstDetectedAt; }
    public Instant getLastDetectedAt() { return lastDetectedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolutionReason() { return resolutionReason; }
    public String getOperatorName() { return operatorName; }
    public Long getMergeAuditId() { return mergeAuditId; }
}
