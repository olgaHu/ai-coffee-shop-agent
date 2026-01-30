package lab.olga.aiassist.infrastructure.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lab.olga.aiassist.domain.menu.MenuItem;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class MenuLoader {

    private final ObjectMapper objectMapper;

    public MenuLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<MenuItem> load() {
        try {
            ClassPathResource res = new ClassPathResource("data/coffeeshop_menu.json");
            try (InputStream in = res.getInputStream()) {
                List<MenuItem> items = objectMapper.readValue(in, new TypeReference<List<MenuItem>>() {});
                basicValidate(items);
                return items;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load coffeeshop_menu.json from classpath", e);
        }
    }

    private void basicValidate(List<MenuItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Menu is empty. Check coffeeshop_menu.json content.");
        }
        // 先只做最輕量的 sanity check：第一筆至少要有 name
        MenuItem first = items.get(0);
        if (first.name() == null || first.name().isBlank()) {
            throw new IllegalStateException("Menu first item missing required field: name");
        }
    }
}