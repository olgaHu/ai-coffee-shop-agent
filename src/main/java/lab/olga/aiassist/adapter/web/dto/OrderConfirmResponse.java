package lab.olga.aiassist.adapter.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmResponse(
        String menuItemId,
        String name,
        BigDecimal unitPrice,
        int qty,
        BigDecimal total,
        String currency,
        List<String> tags,
        String message
) {}