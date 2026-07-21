package com.officeflow.common.api;

import java.util.List;

public record PageResult<T>(long total, long pageNum, long pageSize, List<T> records) {

    public static <T> PageResult<T> of(long total, long pageNum, long pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records);
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(0, pageNum, pageSize, List.of());
    }
}

