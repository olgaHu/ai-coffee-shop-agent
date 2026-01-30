package lab.olga.aiassist;


import lab.olga.aiassist.application.embedding.EmbeddingProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingSmokeTestRunner implements CommandLineRunner {

    private final EmbeddingProvider embeddingProvider;

    public EmbeddingSmokeTestRunner(EmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    @Override
    public void run(String... args) {
        var v = embeddingProvider.embed("hello embeddings");
        System.out.println("embedding size = " + v.size());
        System.out.println("first3 = " + v.subList(0, Math.min(3, v.size())));
    }
}
