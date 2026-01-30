package lab.olga.aiassist.application.rag.ingest;

import lab.olga.aiassist.domain.menu.MenuItem;

import java.util.HashMap;
import java.util.Map;

public class MenuDocumentMapper {
    public static IngestDocument toDoc(MenuItem item) {
        Map<String, Object> md = new HashMap<>();
        md.put("menuItemId", item.id());
        md.put("name", item.name());
        md.put("category", item.category());
        md.put("price", item.price());
        md.put("currency", item.currency());

        String text = """
                品項：%s
                分類：%s
                價格：%s %s
                說明：%s
                標籤：%s
                """.formatted(
                item.name(),
                item.category(),
                item.price(), item.currency(),
                item.description() == null ? "" : item.description(),
                item.tags() == null ? "" : String.join(", ", item.tags())
        );

        return new IngestDocument("menu-" + item.id(), text, md);
    }
}
