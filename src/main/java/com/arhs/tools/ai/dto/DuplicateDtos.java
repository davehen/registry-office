package com.arhs.tools.ai.dto;

import com.arhs.tools.ai.domain.*;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class DuplicateDtos {
    private DuplicateDtos() { }
    public record DismissRequest(Long expectedCaseVersion, String reason, String operatorName) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Unknown request field"); }
    }
    public record MergeRequest(Long expectedCaseVersion, Integer canonicalPersonId,
                               Map<Integer, Long> expectedPersonVersions, Map<String, Integer> fieldSources,
                               String reason, String operatorName) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Unknown request field"); }
    }
    public record ScanResult(int activePersons, int evaluatedCandidatePairs, int matchedPairs,
                             int createdCases, int reopenedCases, int obsoleteCases) { }
    public record CaseView(Long id, long version, int leftPersonId, int rightPersonId,
                           DuplicateCase.Status status, int score, DuplicateCase.Confidence confidence,
                           List<String> reasons, Instant firstDetectedAt, Instant lastDetectedAt,
                           Instant resolvedAt, String resolutionReason, String operatorName, Long mergeAuditId) {
        public static CaseView of(DuplicateCase c) {
            return new CaseView(c.getId(), c.getVersion(), c.getLeftPersonId(), c.getRightPersonId(),
                    c.getStatus(), c.getScore(), c.getConfidence(), c.getReasons(), c.getFirstDetectedAt(),
                    c.getLastDetectedAt(), c.getResolvedAt(), c.getResolutionReason(), c.getOperatorName(), c.getMergeAuditId());
        }
    }
    public record EventView(Long id, String type, Instant occurredAt, String reason, String operatorName) {
        public static EventView of(DuplicateCaseEvent e) {
            return new EventView(e.getId(), e.getType(), e.getOccurredAt(), e.getReason(), e.getOperatorName());
        }
    }
    public record CaseDetail(CaseView duplicateCase, PersonView leftPerson, PersonView rightPerson, List<EventView> events) { }
    public record CasePage(List<CaseView> content, int page, int size, long totalElements, int totalPages) { }
    public record MergeEvidence(PersonView leftBefore, PersonView rightBefore,
                                PersonView canonicalAfter, Map<String, Integer> fieldSources) { }
    public record AuditView(Long id, Long caseId, int canonicalPersonId, int retiredPersonId,
                            Instant mergedAt, String reason, String operatorName, MergeEvidence evidence) { }
}
