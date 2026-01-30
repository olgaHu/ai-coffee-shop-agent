package lab.olga.aiassist.smoketest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lab.olga.aiassist.application.embedding.EmbeddingProvider;
import lab.olga.aiassist.domain.menu.MenuItem;
import lab.olga.aiassist.infrastructure.qdrant.QdrantHttpClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Profile("dev")
@Component
public class MenuIngestTestRunner implements CommandLineRunner {

    private final ObjectMapper om;
    private final EmbeddingProvider embeddingProvider;
    private final QdrantHttpClient qdrant;

    public MenuIngestTestRunner(ObjectMapper om, EmbeddingProvider embeddingProvider, QdrantHttpClient qdrant) {
        this.om = om;
        this.embeddingProvider = embeddingProvider;
        this.qdrant = qdrant;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1) 讀 JSON 檔
        var is = getClass().getResourceAsStream("/data/coffeeshop_menu.json");
        if (is == null) throw new IllegalStateException("coffeeshop_menu.json not found in resources");

        // 2) 解析成 List<MenuItem>（等你貼 JSON 格式後我給你最小 DTO）
        List<MenuItem> items = om.readValue(is, new TypeReference<List<MenuItem>>() {});

        // 3) 只 ingest 前 3 筆（最小可驗證）
        int limit = Math.min(3, items.size());
        for (int i = 0; i < limit; i++) {
            MenuItem item = items.get(i);

            String text = buildText(item);                 // embedding input
            List<Float> vector = embeddingProvider.embed(text);

            Map<String, Object> payload = buildPayload(item, text);

            long pointId = i + 1;                          // 先用簡單 stable id；下一步再換成 hash/雪花
            qdrant.upsert(pointId, vector, payload);
        }

        System.out.println("[MenuIngestRunner] OK, ingested " + limit + " items");
    }

    private String buildText(MenuItem item) {
        // 先用你之前那種組字法即可
        return """
            【%s】%s
            售價：%s %s
            描述：%s
            選項：%s
            標籤：%s
            """.formatted(
                item.category(), item.name(),
                item.price(), item.currency(),
                item.description(),
                String.join(" / ", item.options()),
                String.join(",", item.tags())
        );
    }

    private Map<String, Object> buildPayload(MenuItem item, String text) {
        return Map.of(
                "docType", "menu_item",
                "menuItemId", item.id(),
                "name", item.name(),
                "category", item.category(),
                "price", item.price(),
                "currency", item.currency(),
                "tags", item.tags(),
                "text", text
        );
    }
}
