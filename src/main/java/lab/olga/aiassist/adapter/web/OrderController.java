package lab.olga.aiassist.adapter.web;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import lab.olga.aiassist.adapter.web.dto.OrderConfirmRequest;
import lab.olga.aiassist.adapter.web.dto.OrderConfirmResponse;
import lab.olga.aiassist.infrastructure.qdrant.QdrantHttpClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final QdrantHttpClient qdrantHttpClient;
    private final ObjectMapper objectMapper;

    @PostMapping("/confirm")
    public OrderConfirmResponse confirm(@RequestBody OrderConfirmRequest req) throws Exception {
        if (req.qty() <= 0) {
            throw new IllegalArgumentException("qty must be > 0");
        }

        log.info("order_confirm req menuItemId={}, qty={}", req.menuItemId(), req.qty());

        String raw = qdrantHttpClient.scrollByMenuItemId(req.menuItemId(), 1);
        JsonNode root = objectMapper.readTree(raw);
        JsonNode points = root.path("result").path("points");

        if (!points.isArray() || points.size() == 0) {
            throw new IllegalArgumentException("menuItemId not found: " + req.menuItemId());
        }

        JsonNode payload = points.get(0).path("payload");

        String menuItemId = payload.path("menuItemId").asText();
        String name = payload.path("name").asText();
        BigDecimal unitPrice = new BigDecimal(payload.path("price").asText());
        String currency = payload.path("currency").asText();

        List<String> tags = new ArrayList<>();
        for (JsonNode t : payload.path("tags")) tags.add(t.asText());

        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(req.qty()));

        String msg = "已為你確認：" + name + " x " + req.qty() + "，合計 " + total + " " + currency;

        return new OrderConfirmResponse(menuItemId, name, unitPrice, req.qty(), total, currency, tags, msg);
    }
}

