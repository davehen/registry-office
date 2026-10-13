# AIDM-184: retain merged records and immutable evidence

Status: accepted within the supplied implementation plan. Date: 2026-10-12.

Duplicate resolution transfers unique identifiers and may race with scans, ordinary edits and other resolutions. Operators also need evidence of which values existed before a merge and which sources produced the surviving record.

Retain the retiring Person as a MERGED tombstone with a canonical ID. Clear its unique username/email and flush that change before assigning selected identifiers to the survivor. Ordinary lists/searches/deletion operate on ACTIVE persons. An unrestricted ID lookup supports 410 responses for tombstones and 409 for attempted writes.

Store events and merge evidence as append-only application history using immutable JPA entities and scalar references, without cascading person relationships. An audit contains both complete pre-merge snapshots, the final canonical snapshot, source selection, reason, optional operator and timestamp. Application operations expose no history update/delete route. Active-person deletion retains this evidence, including when a historical canonical target is later unavailable.

Use optimistic versions for persons/cases and perform validation, identifier release/transfer, retirement, related-case resolution, audit and events in one transaction. A private survivor merge counter guarantees a version increment even when selected business values are unchanged. Snapshots of the survivor are taken after flushing its versioned write. Scan guards are singleton process-local guards held through transaction completion; this does not introduce distributed scan coordination.

Database constraints remain the final defense against concurrent identifier claims and duplicate pairs. Any conflict rolls back the whole transaction and becomes a safe 409 response. Person fingerprint encoding uses length-prefixed nullable values and versions before SHA-256 hashing; raw fingerprint inputs are never logged.

Consequences: immutable evidence survives ordinary deletion, but the existing in-memory database remains transient across application restarts. Candidate groups can still become large for common rule keys. Undo, distributed coordination, durable deployment storage and alternate deletion semantics remain outside AIDM-184.
