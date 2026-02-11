package com.mybank.template.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ с пагинацией
 * Используется для endpoints, которые возвращают списки с пагинацией
 * 
 * @param <T> тип элементов в списке
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    
    /**
     * Список элементов на текущей странице
     */
    private List<T> content;
    
    /**
     * Номер текущей страницы (начиная с 0)
     */
    private int pageNumber;
    
    /**
     * Размер страницы
     */
    private int pageSize;
    
    /**
     * Общее количество элементов
     */
    private long totalElements;
    
    /**
     * Общее количество страниц
     */
    private int totalPages;
    
    /**
     * Первая ли это страница
     */
    private boolean first;
    
    /**
     * Последняя ли это страница
     */
    private boolean last;
    
    /**
     * Есть ли следующая страница
     */
    private boolean hasNext;
    
    /**
     * Есть ли предыдущая страница
     */
    private boolean hasPrevious;

    /**
     * Создать PageResponse из Spring Data Page
     */
    public static <T> PageResponse<T> fromPage(org.springframework.data.domain.Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
