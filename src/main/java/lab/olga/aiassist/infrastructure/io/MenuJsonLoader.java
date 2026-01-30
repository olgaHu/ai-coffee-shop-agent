package lab.olga.aiassist.infrastructure.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lab.olga.aiassist.domain.menu.MenuItem;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class MenuJsonLoader {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public MenuJsonLoader(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    public List<MenuItem> loadFromClasspath(String path) {
        Resource resource = resourceLoader.getResource("classpath:" + path);
        try (InputStream is = resource.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<MenuItem>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load menu json: " + path, e);
        }
    }
}