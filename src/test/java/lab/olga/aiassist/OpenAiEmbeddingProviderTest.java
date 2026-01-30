package lab.olga.aiassist;

import lab.olga.aiassist.application.embedding.EmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OpenAiEmbeddingProviderTest {

    @Autowired
    EmbeddingProvider embeddingProvider;

    @Test
    void embedding_should_be_1536_dims_and_finite() {
        String text = """
                【精品咖啡系列】經典拿鐵 (Latte)
                售價：120 元
                描述：精選義式配方豆，口感絲滑。
                選項：全脂奶 / 低脂奶 / 加價 20 元升級燕麥奶
                標籤：咖啡
                """;

        List<Float> v = embeddingProvider.embed(text);

        assertNotNull(v, "embedding vector is null");
        assertEquals(1536, v.size(), "embedding vector size mismatch");

        // 額外的「資料合理性」檢查：避免 NaN / Infinity 混進去
        for (int i = 0; i < v.size(); i++) {
            float x = v.get(i);
            assertFalse(Float.isNaN(x), "embedding contains NaN at index " + i);
            assertFalse(Float.isInfinite(x), "embedding contains Infinity at index " + i);
        }

        System.out.println("OK: embedding size = " + v.size());
        System.out.println("sample[0..4] = " + v.subList(0, 5));
    }
}
