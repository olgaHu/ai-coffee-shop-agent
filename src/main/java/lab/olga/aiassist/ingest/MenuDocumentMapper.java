package lab.olga.aiassist.ingest;


import lab.olga.aiassist.domain.menu.MenuItem;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MenuDocumentMapper {

    private static final String DOC_TYPE = "menu_item";
    private static final String DEFAULT_CURRENCY = "TWD";

    public static IngestDocument toDoc(MenuItem item) {
        String menuItemKey = normalizeKey(item); // e.g. LATTE
        String pointId = stableUuid("menu:" + menuItemKey);

        String currency = (item.currency() == null || item.currency().isBlank())
                ? DEFAULT_CURRENCY
                : item.currency().trim();

        String vectorText = buildVectorText(item, currency);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("docType", DOC_TYPE);
        payload.put("menuItemId", menuItemKey);
        payload.put("name", item.name());
        payload.put("category", item.category());
        payload.put("price", item.price());
        payload.put("currency", currency);
        payload.put("tags", item.tags() == null ? List.of() : item.tags());
        payload.put("text", vectorText); // 方便 debug / search 回來直接顯示

        return new IngestDocument(pointId, vectorText, payload);
    }

    private static String normalizeKey(MenuItem item) {
        // 你現在 MenuItem[id=latte,...]，先用 id 當穩定 key；統一成大寫、底線風格
        String raw = (item.id() == null ? item.name() : item.id());
        return raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private static String stableUuid(String stableKey) {
        return UUID.nameUUIDFromBytes(stableKey.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String buildVectorText(MenuItem item, String currency) {
        String options = (item.options() == null || item.options().isEmpty())
                ? "無"
                : String.join(" / ", item.options());

        String tags = (item.tags() == null || item.tags().isEmpty())
                ? "無"
                : String.join("、", item.tags());

        String desc = (item.description() == null ? "" : item.description().trim());

        return """
                【%s】%s
                售價：%s %s
                描述：%s
                選項：%s
                標籤：%s
                """.formatted(
                nullToEmpty(item.category()),
                nullToEmpty(item.name()),
                item.price(),
                currency,
                desc.isBlank() ? "無" : desc,
                options,
                tags
        ).trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
