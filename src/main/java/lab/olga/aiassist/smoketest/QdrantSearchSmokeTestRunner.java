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
import java.util.List;

//@Profile("dev")
@Component
public class QdrantSearchSmokeTestRunner implements CommandLineRunner {

    @Value("${qdrant.base-url}")
    private String baseUrl;

    @Value("${qdrant.collection}")
    private String collection;

    private final EmbeddingProvider embeddingProvider;

    public QdrantSearchSmokeTestRunner(EmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1) Query text → embedding
        String queryText = "經典拿鐵";
        var vector = embeddingProvider.embed(queryText);

        // 2) vector → JSON array
        String vectorJson = toVectorJson(vector);

        // 3) Search body
        String body = """
{
  "vector": %s,
  "limit": 3,
  "with_payload": true,
  "filter": {
    "must": [
      { "key": "docType", "match": { "value": "menu_item" } }
    ]
  }
}
""".formatted(vectorJson);


        String url = "%s/collections/%s/points/search"
                .formatted(baseUrl, collection);

        HttpClient client = HttpClient.newHttpClient();

        System.out.println("[QdrantSearchSmokeTest] body head = " + body.substring(0, Math.min(200, body.length())));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[QdrantSearchSmokeTest] HTTP " + resp.statusCode());
        System.out.println(resp.body());

        if (resp.statusCode() / 100 != 2) {
            throw new IllegalStateException("Search failed");
        }
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
}
