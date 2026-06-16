package com.xiaohua.novel.chat.application;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaohua.novel.chat.model.ModelChatRequest;
import com.xiaohua.novel.chat.model.ModelChatResponse;
import com.xiaohua.novel.chat.model.ModelOption;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ChatModelGateway {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final ChatModelProperties properties;
    private final ObjectMapper objectMapper;

    public ChatModelGateway(ChatModelProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<ModelOption> modelOptions() {
        List<ModelOption> options = new ArrayList<>();
        for (Map.Entry<String, ChatModelProperties.ProviderProperties> providerEntry
                : properties.getProviders().entrySet()) {
            String provider = providerEntry.getKey();
            ChatModelProperties.ProviderProperties providerProperties = providerEntry.getValue();
            boolean enabled = hasText(providerProperties.getApiKey());
            for (Map.Entry<String, ChatModelProperties.ModelProperties> modelEntry
                    : providerProperties.getModels().entrySet()) {
                ChatModelProperties.ModelProperties modelProperties = modelEntry.getValue();
                options.add(new ModelOption(
                        provider,
                        providerProperties.getDisplayName(),
                        modelEntry.getKey(),
                        modelProperties.getDisplayName(),
                        modelProperties.isFreeTier(),
                        enabled,
                        enabled
                                ? modelProperties.getDescription()
                                : modelProperties.getDescription() + "（未配置 API Key）"));
            }
        }
        return options;
    }

    public boolean supports(String provider, String model) {
        ChatModelProperties.ProviderProperties providerProperties =
                properties.getProviders().get(provider);
        return providerProperties != null && providerProperties.getModels().containsKey(model);
    }

    public ModelChatResponse complete(ModelChatRequest request) {
        ChatModelProperties.ProviderProperties provider = properties
                .getProviders()
                .get(request.provider());
        if (provider == null) {
            throw new ChatModelException("MODEL_PROVIDER_NOT_SUPPORTED", "不支持的模型供应商");
        }
        if (!provider.getModels().containsKey(request.model())) {
            throw new ChatModelException("MODEL_NOT_SUPPORTED", "当前供应商不支持该模型");
        }
        if (!hasText(provider.getApiKey())) {
            throw new ChatModelException("MODEL_API_KEY_MISSING", "后端没有配置该模型供应商的 API Key");
        }

        RestClient restClient = restClient(provider.getBaseUrl());
        Map<String, Object> body = Map.of(
                "model", request.model(),
                "messages", request.messages(),
                "stream", false,
                "temperature", 0.82,
                "max_tokens", 1600);

        long started = System.nanoTime();
        try {
            JsonNode root = restClient
                    .post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null || root.path("choices").isEmpty()) {
                throw new ChatModelException("MODEL_RESPONSE_EMPTY", "模型没有返回可用回复");
            }
            JsonNode choice = root.path("choices").path(0);
            String content = choice.path("message").path("content").asText("");
            if (!hasText(content)) {
                content = choice.path("delta").path("content").asText("");
            }
            if (!hasText(content)) {
                throw new ChatModelException("MODEL_RESPONSE_EMPTY", "模型回复为空");
            }

            JsonNode usage = root.path("usage");
            if (usage.isMissingNode() || usage.isNull()) {
                usage = choice.path("usage");
            }
            long promptTokens = usage.path("prompt_tokens").asLong(estimateTokens(request));
            long completionTokens = usage.path("completion_tokens").asLong(estimateTokens(content));
            long totalTokens = usage.path("total_tokens").asLong(promptTokens + completionTokens);

            return new ModelChatResponse(
                    request.provider(),
                    request.model(),
                    content,
                    promptTokens,
                    completionTokens,
                    totalTokens,
                    Duration.ofNanos(System.nanoTime() - started).toMillis());
        } catch (RestClientResponseException exception) {
            throw new ChatModelException(
                    "MODEL_HTTP_ERROR",
                    "模型接口调用失败：" + readableError(exception),
                    exception);
        } catch (ChatModelException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ChatModelException(
                    "MODEL_CALL_FAILED",
                    "模型接口调用异常：" + exception.getMessage(),
                    exception);
        }
    }

    public String defaultProvider() {
        return properties.getDefaultProvider();
    }

    public String defaultModel() {
        return properties.getDefaultModel();
    }

    private RestClient restClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = Math.max(5, properties.getTimeoutSeconds()) * 1000;
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);
        return RestClient.builder()
                .baseUrl(stripTrailingSlash(baseUrl))
                .requestFactory(requestFactory)
                .build();
    }

    private String readableError(RestClientResponseException exception) {
        String body = exception.getResponseBodyAsString();
        if (!hasText(body)) {
            return exception.getStatusCode().toString();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String message = root.path("error").path("message").asText("");
            if (hasText(message)) {
                return message;
            }
            return root.toString();
        } catch (Exception ignored) {
            return body.length() > 500 ? body.substring(0, 500) : body;
        }
    }

    private static long estimateTokens(Object value) {
        return Math.max(1, String.valueOf(value).length() / 4L);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
