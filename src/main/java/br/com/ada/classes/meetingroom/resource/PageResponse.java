package br.com.ada.classes.meetingroom.resource;

import br.com.ada.classes.meetingroom.model.PageResult;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <D, R> PageResponse<R> from(PageResult<D> result, Function<D, R> mapper) {
        List<R> mapped = result.content().stream().map(mapper).toList();
        return new PageResponse<>(mapped, result.page(), result.size(),
                result.totalElements(), result.totalPages());
    }
}

