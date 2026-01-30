package lab.olga.aiassist.application.embedding;
import java.util.List;

public interface EmbeddingProvider {
    List<Float> embed(String input);
    List<List<Float>> embedBatch(List<String> texts);

}