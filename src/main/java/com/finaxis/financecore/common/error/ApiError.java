package com.finaxis.financecore.common.error;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId,
        Map<String, String> fieldErrors
) {
}
