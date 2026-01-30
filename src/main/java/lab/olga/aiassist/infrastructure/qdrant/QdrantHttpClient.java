package lab.olga.aiassist.infrastructure.qdrant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class QdrantHttpClient {

    private final String baseUrl;
    private final String collection;
    private final HttpClient httpClient;

    public QdrantHttpClient(
            @Value("${qdrant.base-url}") String baseUrl,
            @Value("${qdrant.collection}") String collection
    ) {
        this.baseUrl = baseUrl;
        this.collection = collection;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public void upsertPoint(String pointId, List<Float> vector, Map<String, Object> payload) {
        try {
            String vectorJson = toVectorJson(vector);
            String payloadJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(payload);

            String body = """
                {
                  "points": [
                    {
                      "id": "%s",
                      "vector": %s,
                      "payload": %s
                    }
                  ]
                }
                """.formatted(pointId, vectorJson, payloadJson);

            String url = "%s/collections/%s/points?wait=true"
                    .formatted(baseUrl, collection);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() / 100 != 2) {
                throw new IllegalStateException("Qdrant upsert failed: " + resp.body());
            }

        } catch (Exception e) {
            throw new RuntimeException("Qdrant upsert error", e);
        }
    }

    public void upsert(long pointId, List<Float> vector, Map<String, Object> payload) {
        this.upsertPoint(String.valueOf(pointId), vector, payload);
    }

    private boolean isNumeric(String str) {
        return str != null && str.matches("\\d+");
    }

    private static String toVectorJson(List<Float> vector) {
        StringBuilder sb = new StringBuilder(vector.size() * 10);
        sb.append('[');
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) sb.append(',');
            float x = vector.get(i);
            if (Float.isNaN(x) || Float.isInfinite(x)) x = 0.0f;
            sb.append(x);
        }
        sb.append(']');
        return sb.toString();
    }

    public int countPoints() {
        try {
            String url = "%s/collections/%s/points/count"
                    .formatted(baseUrl, collection);

            String body = "{\"exact\":true}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (resp.statusCode() / 100 != 2) {
                throw new IllegalStateException("Qdrant count failed: " + resp.body());
            }

            // 最小解析：用 Jackson 取 result.count
            var root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp.body());
            return root.path("result").path("count").asInt(-1);

        } catch (Exception e) {
            throw new RuntimeException("Qdrant count error", e);
        }
    }

    public String searchTopK(List<Float> vector, int limit) {
        try {
            String body = """
        {
          "vector": %s,
          "limit": %d,
          "with_payload": true
        }
        """.formatted(toVectorJson(vector), limit);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collection + "/points/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("Qdrant search failed: status=" + resp.statusCode() + ", body=" + resp.body());
            }
            return resp.body();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant search error", e);
        }
    }

    public String searchTopKWithTag(List<Float> vector, String tag, int limit) {
        try {
            String body = """
        {
          "vector": %s,
          "limit": %d,
          "with_payload": true,
          "filter": {
            "must": [
              {
                "key": "tags",
                "match": {
                  "any": ["%s"]
                }
              }
            ]
          }
        }
        """.formatted(
                    toVectorJson(vector),
                    limit,
                    tag
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collection + "/points/search"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("Qdrant search failed: status=" + resp.statusCode() + ", body=" + resp.body());
            }
            return resp.body();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant search with tag error", e);
        }
    }

    public String scrollByMenuItemId(String menuItemId, int limit) {
        try {
            String body = """
        {
          "limit": %d,
          "with_payload": true,
          "filter": {
            "must": [
              { "key": "menuItemId", "match": { "value": "%s" } }
            ]
          }
        }
        """.formatted(limit, menuItemId);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collection + "/points/scroll"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("Qdrant scroll failed: status=" + resp.statusCode() + ", body=" + resp.body());
            }
            return resp.body();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant scroll error", e);
        }
    }



}
