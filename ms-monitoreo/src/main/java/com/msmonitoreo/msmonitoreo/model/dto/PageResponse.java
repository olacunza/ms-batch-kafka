package com.msmonitoreo.msmonitoreo.model.dto;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        long totalPages) {
            public static <T> PageResponse<T> of(List<T> content, int page, int size, long total) {
                return new PageResponse<>(List.copyOf(content), page, size, total, total / size + (total % size == 0 ? 0 : 1));
            }
            public <R> PageResponse<R> map(Function<T,R> mapper) {
                return new PageResponse<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
            }
        }
