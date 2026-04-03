package com.xs.bbs.common;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long total,
        int pageNo,
        int pageSize,
        long totalPages,
        boolean hasPrevious,
        boolean hasNext
) {
}
