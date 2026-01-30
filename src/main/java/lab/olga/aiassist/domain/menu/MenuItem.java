package lab.olga.aiassist.domain.menu;

import java.math.BigDecimal;
import java.util.List;

public record MenuItem(
        String id,
        String name,
        String category,
        BigDecimal price,
        String currency,
        String description,
        List<String> options,
        List<String> tags
) {}
