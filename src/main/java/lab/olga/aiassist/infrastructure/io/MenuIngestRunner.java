package lab.olga.aiassist.infrastructure.io;

import lab.olga.aiassist.application.embedding.EmbeddingProvider;
import lab.olga.aiassist.infrastructure.qdrant.QdrantHttpClient;
import lab.olga.aiassist.ingest.IngestDocument;
import lab.olga.aiassist.ingest.MenuDocumentMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MenuIngestRunner implements CommandLineRunner {

    private final MenuLoader menuLoader;
    private final EmbeddingProvider openAiEmbeddingProvider;
    private final QdrantHttpClient qdrantHttpClient; // 你可以沿用你現在 HttpClient 呼叫方式封裝一下


    public MenuIngestRunner(MenuLoader menuLoader , EmbeddingProvider embeddingProvider, QdrantHttpClient qdrant) {
        this.menuLoader = menuLoader;
        this.openAiEmbeddingProvider = embeddingProvider;
        this.qdrantHttpClient = qdrant;
    }

    @Override
    public void run(String... args) {
        var items = menuLoader.load();
        System.out.println("[MenuIngestRunner] Loaded menu items: " + items.size());
        System.out.println("[MenuIngestRunner] First item: " + items.get(0));

        var first = items.get(0);
//        var doc = MenuDocumentMapper.toDoc(first);
        var docs = items.stream()
                .map(MenuDocumentMapper::toDoc)
                .toList();

        System.out.println("[MenuIngestRunner] docs=" + docs.size());

        var texts = docs.stream().map(IngestDocument::vectorText).toList();

        var vectors = openAiEmbeddingProvider.embedBatch(texts);

        System.out.println("[MenuIngestRunner] vectors=" + vectors.size());
        System.out.println("[MenuIngestRunner] first vector size=" + vectors.get(0).size());
        System.out.println("[MenuIngestRunner] first doc pointId=" + docs.get(0).pointId());
        System.out.println("[MenuIngestRunner] first3=" + vectors.get(0).subList(0, 3));

        int batchSize =  docs.size();
        int n = Math.min(batchSize, docs.size());

        for (int i = 0; i < n; i++) {
            var doc = docs.get(i);
            var vector = vectors.get(i);

            qdrantHttpClient.upsertPoint(doc.pointId(), vector, doc.payload());

            System.out.println("[MenuIngestRunner] upserted i=" + i
                    + ", pointId=" + doc.pointId()
                    + ", menuItemId=" + doc.payload().get("menuItemId"));
        }

        System.out.println("[MenuIngestRunner] upserted points=" + n);

        int count = qdrantHttpClient.countPoints();
        System.out.println("[MenuIngestRunner] Qdrant count after upsert=" + count);


    }
}
