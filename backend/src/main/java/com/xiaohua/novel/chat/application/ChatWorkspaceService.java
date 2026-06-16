package com.xiaohua.novel.chat.application;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.xiaohua.novel.chat.api.CreateConversationRequest;
import com.xiaohua.novel.chat.api.SendChatMessageRequest;
import com.xiaohua.novel.chat.api.UpdateUiPreferenceRequest;
import com.xiaohua.novel.chat.infrastructure.ChatWorkspaceRepository;
import com.xiaohua.novel.chat.model.ChatConversationRecord;
import com.xiaohua.novel.chat.model.ChatMessageRecord;
import com.xiaohua.novel.chat.model.ModelChatMessage;
import com.xiaohua.novel.chat.model.ModelChatRequest;
import com.xiaohua.novel.chat.model.ModelChatResponse;
import com.xiaohua.novel.chat.model.ModelOption;
import com.xiaohua.novel.chat.model.UiPreferenceRecord;
import org.springframework.stereotype.Service;

@Service
public class ChatWorkspaceService {

    private static final String LOCAL_USER_ID = "local-user";
    private static final int HISTORY_LIMIT = 18;
    private static final String SYSTEM_PROMPT = """
            你是“小花无限小说工坊”的小说总导演。
            你的职责不是泛泛聊天，而是帮助十七八岁的年轻创作者把一个灵感变成能长期连载的小说项目。
            回答要中文、清爽、有审美，优先给出可执行的方向、选项和下一步。
            当用户在讨论小说时，你要关注题材、读者期待、人物弧光、爽点、钩子、连续性和章节推进。
            当前阶段可以讨论与生成创意，但不要假装已经调用不存在的自动写作流水线。
            """;

    private final ChatWorkspaceRepository repository;
    private final ChatModelGateway modelGateway;

    public ChatWorkspaceService(
            ChatWorkspaceRepository repository,
            ChatModelGateway modelGateway) {
        this.repository = repository;
        this.modelGateway = modelGateway;
    }

    public List<ModelOption> modelOptions() {
        return modelGateway.modelOptions();
    }

    public UiPreferenceRecord getPreference() {
        return repository.findPreference(LOCAL_USER_ID)
                .orElseGet(() -> repository.upsertPreference(
                        LOCAL_USER_ID,
                        "peach",
                        "#ff6ea8",
                        false,
                        modelGateway.defaultProvider(),
                        modelGateway.defaultModel()));
    }

    public UiPreferenceRecord updatePreference(UpdateUiPreferenceRequest request) {
        requireSupportedModel(request.defaultProvider(), request.defaultModel());
        return repository.upsertPreference(
                LOCAL_USER_ID,
                request.themeId().trim(),
                request.customAccentColor().trim(),
                request.sidebarCollapsed(),
                request.defaultProvider().trim(),
                request.defaultModel().trim());
    }

    public List<ChatConversationRecord> conversations() {
        return repository.findConversations(LOCAL_USER_ID);
    }

    public ChatConversationRecord createConversation(CreateConversationRequest request) {
        UiPreferenceRecord preference = getPreference();
        String provider = normalizeOrDefault(request.provider(), preference.defaultProvider());
        String model = normalizeOrDefault(request.model(), preference.defaultModel());
        requireSupportedModel(provider, model);
        return repository.createConversation(
                LOCAL_USER_ID,
                normalizeOrDefault(request.title(), "新的小说灵感"),
                provider,
                model);
    }

    public ChatConversationRecord getConversation(String conversationId) {
        return repository.findConversation(LOCAL_USER_ID, conversationId)
                .orElseThrow(() -> new ChatWorkspaceNotFoundException("聊天会话不存在"));
    }

    public List<ChatMessageRecord> messages(String conversationId) {
        getConversation(conversationId);
        return repository.findMessages(conversationId);
    }

    public SendResult sendMessage(SendChatMessageRequest request) {
        UiPreferenceRecord preference = getPreference();
        String provider = normalizeOrDefault(request.provider(), preference.defaultProvider());
        String model = normalizeOrDefault(request.model(), preference.defaultModel());
        requireSupportedModel(provider, model);

        ChatConversationRecord conversation = resolveConversation(request, provider, model);
        ChatMessageRecord userMessage = repository.saveMessage(
                conversation.id(),
                "user",
                request.content().trim(),
                null,
                null,
                0,
                0,
                0,
                null,
                null);
        repository.touchConversation(conversation.id(), provider, model);

        List<ModelChatMessage> modelMessages = buildModelMessages(conversation.id());
        long started = System.nanoTime();
        try {
            ModelChatResponse modelResponse = modelGateway.complete(new ModelChatRequest(
                    provider,
                    model,
                    modelMessages));
            ChatMessageRecord assistantMessage = repository.saveMessage(
                    conversation.id(),
                    "assistant",
                    modelResponse.content(),
                    provider,
                    model,
                    modelResponse.promptTokens(),
                    modelResponse.completionTokens(),
                    modelResponse.totalTokens(),
                    null,
                    null);
            repository.recordModelCallSuccess(
                    conversation.id(),
                    userMessage.id(),
                    assistantMessage.id(),
                    modelResponse);
            ChatConversationRecord updatedConversation = getConversation(conversation.id());
            return new SendResult(updatedConversation, userMessage, assistantMessage);
        } catch (ChatModelException exception) {
            repository.recordModelCallFailure(
                    conversation.id(),
                    userMessage.id(),
                    provider,
                    model,
                    Duration.ofNanos(System.nanoTime() - started).toMillis(),
                    exception.code(),
                    exception.getMessage());
            throw exception;
        }
    }

    private ChatConversationRecord resolveConversation(
            SendChatMessageRequest request,
            String provider,
            String model) {
        String conversationId = normalize(request.conversationId());
        if (conversationId == null) {
            return repository.createConversation(
                    LOCAL_USER_ID,
                    titleFromMessage(request.content()),
                    provider,
                    model);
        }
        return getConversation(conversationId);
    }

    private List<ModelChatMessage> buildModelMessages(String conversationId) {
        List<ModelChatMessage> modelMessages = new ArrayList<>();
        modelMessages.add(new ModelChatMessage("system", SYSTEM_PROMPT));
        for (ChatMessageRecord message : repository.findRecentMessages(conversationId, HISTORY_LIMIT)) {
            if ("user".equals(message.role()) || "assistant".equals(message.role())) {
                modelMessages.add(new ModelChatMessage(message.role(), message.content()));
            }
        }
        return modelMessages;
    }

    private void requireSupportedModel(String provider, String model) {
        if (!modelGateway.supports(provider, model)) {
            throw new ChatModelException("MODEL_NOT_SUPPORTED", "当前模型未在后端配置中开放");
        }
    }

    private static String titleFromMessage(String content) {
        String compact = content.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 18) {
            return compact;
        }
        return compact.substring(0, 18) + "...";
    }

    private static String normalizeOrDefault(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized == null ? defaultValue : normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record SendResult(
            ChatConversationRecord conversation,
            ChatMessageRecord userMessage,
            ChatMessageRecord assistantMessage) {
    }
}
