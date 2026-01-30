package lab.olga.aiassist.smoketest;

import lab.olga.aiassist.application.embedding.EmbeddingProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Profile("dev")
@Component
public class QdrantUpsertSmokeTestRunner implements CommandLineRunner {

    @Value("${qdrant.base-url}")
    private String baseUrl;

    @Value("${qdrant.collection}")
    private String collection;

    private final EmbeddingProvider embeddingProvider;

    public QdrantUpsertSmokeTestRunner(EmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    @Override
    public void run(String... args) throws Exception {
        // 0) 準備要 embedding 的文字（之後 ingest menu.json 也會用同樣組字策略）
        String text = """
                【精品咖啡系列】經典拿鐵 (Latte)
                售價：120 元
                描述：精選義式配方豆，口感絲滑。
                選項：全脂奶 / 低脂奶 / 加價 20 元升級燕麥奶
                標籤：咖啡
                """;

        // 1) 取得真實 embedding（1536 維）
        List<Float> vector = embeddingProvider.embed(text);
        if (vector == null || vector.size() != 1536) {
            throw new IllegalStateException("Embedding size mismatch: " + (vector == null ? "null" : vector.size()));
        }
        String vectorJson = toVectorJson(vector);

        // 2) 固定 payload schema（v0）
        String payloadJson = """
                {
                  "docType": "menu_item",
                  "menuItemId": "LATTE_CLASSIC",
                  "name": "經典拿鐵",
                  "category": "精品咖啡系列",
                  "price": 120,
                  "currency": "TWD",
                  "tags": ["咖啡"],
                  "text": %s
                }
                """.formatted(jsonEscapeAsJsonString(text));

        // 3) Qdrant upsert body（point id 先沿用 1）
        String body = """
                {
                  "points": [
                    {
                      "id": 1,
                      "vector": %s,
                      "payload": %s
                    }
                  ]
                }
                """.formatted(vectorJson, payloadJson);

        // 4) 呼叫 Qdrant: PUT /collections/{collection}/points?wait=true
        String url = "%s/collections/%s/points?wait=true".formatted(baseUrl, collection);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        System.out.println("[QdrantUpsertSmokeTest] HTTP " + resp.statusCode());
        System.out.println(resp.body());

        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("Upsert failed, status=" + resp.statusCode() + ", body=" + resp.body());
        }

        System.out.println("[QdrantUpsertSmokeTest] OK upsert with REAL embedding, dim=" + vector.size());
        System.out.println("[QdrantUpsertSmokeTest] vector first3=" + vector.subList(0, 3));
    }

    /** 把 List<Float> 轉成 Qdrant vector JSON array，例如 [0.1, -0.2, ...] */
    private static String toVectorJson(List<Float> vector) {
        StringBuilder sb = new StringBuilder(vector.size() * 10);
        sb.append('[');
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(',');
            Float v = vector.get(i);
            if (v == null) {
                sb.append("0.0");
            } else {
                // 避免 NaN/Infinity 破壞 JSON
                float x = v;
                if (Float.isNaN(x) || Float.isInfinite(x)) x = 0.0f;
                sb.append(x);
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /** 將任意文字轉為合法 JSON string（含雙引號），例如 "hello\nworld" */
    private static String jsonEscapeAsJsonString(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
