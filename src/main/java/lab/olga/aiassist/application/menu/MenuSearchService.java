package lab.olga.aiassist.application.menu;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import lab.olga.aiassist.application.embedding.EmbeddingProvider;
import lab.olga.aiassist.infrastructure.qdrant.QdrantHttpClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuSearchService {

    private final EmbeddingProvider embeddingProvider;
    private final QdrantHttpClient qdrantHttpClient;

    public String searchRaw(String q, @Nullable String tag, int limit) {
        List<Float> vec = embeddingProvider.embed(q);

        if (tag == null || tag.isBlank()) {
            return qdrantHttpClient.searchTopK(vec, limit);
        }
        return qdrantHttpClient.searchTopKWithTag(vec, tag.trim(), limit);
    }
}
