package lab.olga.aiassist.smoketest;

import lab.olga.aiassist.application.embedding.EmbeddingProvider;
import lab.olga.aiassist.application.menu.MenuSearchService;
import lab.olga.aiassist.infrastructure.qdrant.QdrantHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class MenuSearchSmokeRunner implements CommandLineRunner {

    private final EmbeddingProvider embeddingProvider;
    private final QdrantHttpClient qdrantHttpClient;

    private final MenuSearchService menuSearchService;


    public MenuSearchSmokeRunner(
            EmbeddingProvider embeddingProvider,
            QdrantHttpClient qdrantHttpClient,
            MenuSearchService menuSearchService
    ) {
        this.embeddingProvider = embeddingProvider;
        this.qdrantHttpClient = qdrantHttpClient;
        this.menuSearchService = menuSearchService;
    }

//    @Override
//    public void run(String... args) {
//        log.info("=== MenuSearchSmokeRunner start ===");
//
//        List<Float> vec = embeddingProvider.embed("巧克力");
//        String result = qdrantHttpClient.searchTopK(vec, 5);
//
//        log.info("Qdrant search result (first 500 chars): {}",
//                result.substring(0, Math.min(500, result.length())));
//
//        log.info("=== MenuSearchSmokeRunner end ===");
//
//        String hot = qdrantHttpClient.searchTopKWithTag(vec, "熱門", 5);
//        log.info("!!! HOT result: {}", hot.substring(0, Math.min(300, hot.length())));
//
//        String hand = qdrantHttpClient.searchTopKWithTag(vec, "手沖", 5);
//        log.info("!!! HAND result: {}", hand.substring(0, Math.min(300, hand.length())));
//    }

    @Override
    public void run(String... args) {
        String r1 = menuSearchService.searchRaw("拿鐵", "熱門", 5);
        log.info("service result (HOT): {}", r1.substring(0, Math.min(300, r1.length())));

        String r2 = menuSearchService.searchRaw("拿鐵", "手沖", 5);
        log.info("service result (HAND): {}", r2.substring(0, Math.min(300, r2.length())));
    }
}

