package lab.olga.aiassist.adapter.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lab.olga.aiassist.application.menu.MenuSearchService;
import lab.olga.aiassist.infrastructure.qdrant.QdrantHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Slf4j
public class MenuSearchController {

    private final MenuSearchService menuSearchService;
    private final ObjectMapper objectMapper;
    private final QdrantHttpClient qdrantHttpClient;

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


}