package lab.olga.aiassist.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiClientConfig {

//    @Bean
//    public WebClient openAiWebClient(OpenAiProperties props) {
//        return WebClient.builder()
//                .baseUrl(props.baseUrl())
//                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
//                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .build();
//    }

    @Bean
    public WebClient openAiWebClient(OpenAiProperties props) {

        int maxBytes = 2 * 1024 * 1024; // 2MB
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(maxBytes))
                .build();

        // 最小防呆：避免空 key 默默進去，最後才 401
        String key = props.apiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is missing/blank (props.apiKey() is empty)");
        }

        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter((request, next) -> {
                    boolean hasAuth = request.headers().containsKey(HttpHeaders.AUTHORIZATION);
                    String authValue = request.headers().getFirst(HttpHeaders.AUTHORIZATION);
                    // 只印前綴與長度，不印 key 本體
                    String authInfo = (authValue == null)
                            ? "null"
                            : (authValue.startsWith("Bearer ") ? "Bearer <redacted>, len=" + authValue.length() : "non-bearer, len=" + authValue.length());
                    System.out.println("[OpenAI] " + request.method() + " " + request.url() + " hasAuth=" + hasAuth + " auth=" + authInfo);
                    return next.exchange(request);
                })
                .build();
    }
}