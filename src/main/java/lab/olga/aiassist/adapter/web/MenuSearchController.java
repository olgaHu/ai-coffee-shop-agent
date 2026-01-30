package lab.olga.aiassist.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lab.olga.aiassist.adapter.web.dto.OrderConfirmRequest;
import lab.olga.aiassist.application.menu.MenuSearchService;
import lab.olga.aiassist.infrastructure.qdrant.QdrantHttpClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuSearchController {

    private final MenuSearchService menuSearchService;
    private final ObjectMapper objectMapper;
    private final QdrantHttpClient qdrantHttpClient;

//    @GetMapping("/search")
//    public String search(
//            @RequestParam("q") String q,
//            @RequestParam(value = "tag", required = false) String tag,
//            @RequestParam(value = "limit", defaultValue = "5") int limit
//    ) {
//        return menuSearchService.searchRaw(q, tag, limit);
//    }

    @GetMapping("/search")
    public List<JsonNode> search(
            @RequestParam("q") String q,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) throws Exception {

        String raw = menuSearchService.searchRaw(q, tag, limit);

        JsonNode root = objectMapper.readTree(raw);
        JsonNode points = root.path("result");

        List<JsonNode> payloads = new ArrayList<>();
        for (JsonNode p : points) {
            JsonNode payload = p.path("payload");
            if (!payload.isMissingNode() && !payload.isNull()) {
                payloads.add(payload);
            }
        }
        return payloads;
    }

    public static class OrderConfirmController {
    }


    @PostMapping("/confirm")
    public Map<String, Object> confirm(@RequestBody OrderConfirmRequest req) throws Exception {



        if (req.menuItemId() == null || req.menuItemId().isBlank()) {
            throw new IllegalArgumentException("menuItemId is required");
        }
        if (req.qty() <= 0) {
            throw new IllegalArgumentException("qty must be > 0");
        }

        String raw = qdrantHttpClient.scrollByMenuItemId(req.menuItemId().trim(), 1);
        JsonNode root = objectMapper.readTree(raw);

        JsonNode points = root.path("result").path("points");
        if (!points.isArray() || points.size() == 0) {
            throw new IllegalArgumentException("menuItemId not found: " + req.menuItemId());
        }

        JsonNode payload = points.get(0).path("payload");

        String menuItemId = payload.path("menuItemId").asText();
        String name = payload.path("name").asText();
        BigDecimal unitPrice = new BigDecimal(payload.path("price").asText("0"));
        String currency = payload.path("currency").asText("TWD");

        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(req.qty()));

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("menuItemId", menuItemId);
        resp.put("name", name);
        resp.put("unitPrice", unitPrice);
        resp.put("qty", req.qty());
        resp.put("total", total);
        resp.put("currency", currency);
        resp.put("message", "已確認：" + name + " x " + req.qty() + "，合計 " + total + " " + currency);

        return resp;
    }
}