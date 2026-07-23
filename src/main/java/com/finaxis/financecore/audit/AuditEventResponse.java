package com.finaxis.financecore.audit;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        Long actorId,
        String action,
        String resourceType,
        String resourceId,
        Instant createdAt
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActorId(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getCreatedAt()
        );
    }
}
