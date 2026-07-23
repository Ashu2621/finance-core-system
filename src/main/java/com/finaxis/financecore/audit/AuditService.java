package com.finaxis.financecore.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository repository;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long actorId, String action, String resourceType, Object resourceId) {
        AuditEvent event = new AuditEvent();
        event.setActorId(actorId);
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId == null ? null : resourceId.toString());
        event.setCreatedAt(Instant.now(clock));
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> list(int page, int size) {
        return repository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(AuditEventResponse::from);
    }
}
