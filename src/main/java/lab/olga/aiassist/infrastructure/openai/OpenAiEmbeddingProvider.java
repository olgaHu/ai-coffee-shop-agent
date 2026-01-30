package lab.olga.aiassist.infrastructure.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lab.olga.aiassist.application.embedding.EmbeddingProvider;
import lab.olga.aiassist.config.OpenAiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiEmbeddingProvider implements EmbeddingProvider {

    private final WebClient webClient;
    private final OpenAiProperties props;

    public OpenAiEmbeddingProvider(WebClient openAiWebClient, OpenAiProperties props) {
        this.webClient = openAiWebClient;
        this.props = props;
    }

    @Override
    public List<Float> embed(String input) {
        var req = new EmbeddingsRequest(props.embeddingsModel(), input);

        try {
            EmbeddingsResponse resp = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(EmbeddingsResponse.class)
                    .retryWhen(
                            Retry.fixedDelay(3, Duration.ofSeconds(2))
                                    .filter(ex -> ex instanceof WebClientResponseException wex
                                            && wex.getStatusCode().is5xxServerError())
                    )
                    .block();

            if (resp == null || resp.data == null || resp.data.isEmpty()) {
                throw new IllegalStateException("OpenAI embeddings response is empty");
            }

            var first = resp.data.get(0);
            if (first.embedding == null || first.embedding.isEmpty()) {
                throw new IllegalStateException("OpenAI embeddings returned empty vector");
            }
            return first.embedding;

        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "OpenAI embeddings failed: status=" + e.getRawStatusCode()
                            + ", body=" + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    @Override
    public List<List<Float>> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        var req = new EmbeddingsRequest(props.embeddingsModel(), texts);

        try {
            EmbeddingsResponse resp = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(EmbeddingsResponse.class)
                    .retryWhen(
                            Retry.fixedDelay(3, Duration.ofSeconds(2))
                                    .filter(ex -> ex instanceof WebClientResponseException wex
                                            && wex.getStatusCode().is5xxServerError())
                    )
                    .block();

            if (resp == null || resp.data == null || resp.data.isEmpty()) {
                throw new IllegalStateException("OpenAI embeddings response is empty");
            }

            // OpenAI 通常會依 index / input 順序回來。最小實作先假設回來順序與 input 一致。
            List<List<Float>> result = new ArrayList<>(resp.data.size());
            for (EmbeddingsResponse.Item item : resp.data) {
                if (item.embedding == null || item.embedding.isEmpty()) {
                    throw new IllegalStateException("OpenAI embeddings returned empty vector at index=" + item.index);
                }
                result.add(item.embedding);
            }

            // 基本 sanity check：筆數對齊
            if (result.size() != texts.size()) {
                throw new IllegalStateException(
                        "OpenAI embeddings size mismatch: inputs=" + texts.size() + ", outputs=" + result.size()
                );
            }

            return result;

        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "OpenAI embeddings(batch) failed: status=" + e.getRawStatusCode()
                            + ", body=" + e.getResponseBodyAsString(),
                    e
            );
        }
    }

    // ====== DTOs (minimal) ======
    /**
     * OpenAI embeddings 的 input 支援 string 或 array of strings。
     * 用 Object 讓 Jackson 自動序列化成 "input": "..." 或 "input": ["...","..."]
     */
    record EmbeddingsRequest(String model, @JsonProperty("input") Object input) {}

    static class EmbeddingsResponse {
        public List<Item> data;

        @JsonProperty("model")
        public String model;

        static class Item {
            public List<Float> embedding;
            public int index;
        }
    }
}
