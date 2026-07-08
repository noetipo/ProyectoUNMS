package unmsm.edu.pe.shared.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta paginada genérica: { content, total, page, size }.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private long total;
    private int page;
    private int size;

    public static <T> PageResponse<T> of(List<T> content, long total, int page, int size) {
        return PageResponse.<T>builder()
                .content(content)
                .total(total)
                .page(page)
                .size(size)
                .build();
    }
}
