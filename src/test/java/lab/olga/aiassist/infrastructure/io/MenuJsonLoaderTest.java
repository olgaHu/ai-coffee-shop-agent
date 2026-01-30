package lab.olga.aiassist.infrastructure.io;
import lab.olga.aiassist.infrastructure.io.MenuJsonLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MenuJsonLoaderTest {

    @Autowired
    MenuJsonLoader loader;

    @Test
    void load_ok() {
        var items = loader.loadFromClasspath("coffeeshop_menu.json");
        assertThat(items).isNotEmpty();
    }
}