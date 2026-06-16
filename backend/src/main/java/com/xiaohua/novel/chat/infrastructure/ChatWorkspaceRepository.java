package com.xiaohua.novel.chat.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.xiaohua.novel.chat.model.ChatConversationRecord;
import com.xiaohua.novel.chat.model.ChatMessageRecord;
import com.xiaohua.novel.chat.model.ModelChatResponse;
import com.xiaohua.novel.chat.model.UiPreferenceRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ChatWorkspaceRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatWorkspaceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UiPreferenceRecord> findPreference(String userId) {
        List<UiPreferenceRecord> result = jdbcTemplate.query(
                """
                SELECT user_id, theme_id, custom_accent_color, sidebar_collapsed,
                       default_provider, default_model
                FROM user_ui_preference
                WHERE user_id = ?
                """,
                this::mapPreference,
                userId);
        return result.stream().findFirst();
    }

    @Transactional
    public UiPreferenceRecord upsertPreference(
            String userId,
            String themeId,
            String customAccentColor,
            boolean sidebarCollapsed,
            String provider,
            String model) {
        jdbcTemplate.update(
                """
                INSERT INTO user_ui_preference (
                    user_id, theme_id, custom_accent_color, sidebar_collapsed,
                    default_provider, default_model
                ) VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    theme_id = VALUES(theme_id),
                    custom_accent_color = VALUES(custom_accent_color),
                    sidebar_collapsed = VALUES(sidebar_collapsed),
                    default_provider = VALUES(default_provider),
                    default_model = VALUES(default_model)
                """,
                userId,
                themeId,
                customAccentColor,
                sidebarCollapsed,
                provider,
                model);
        return findPreference(userId)
                .orElseThrow(() -> new IllegalStateException("偏好设置保存失败"));
    }

    public List<ChatConversationRecord> findConversations(String userId) {
        return jdbcTemplate.query(
                """
                SELECT id, user_id, title, provider, model_name, archived,
                       created_at, updated_at
                FROM chat_conversation
                WHERE user_id = ? AND archived = FALSE
                ORDER BY updated_at DESC
                LIMIT 50
                """,
                this::mapConversation,
                userId);
    }

    public Optional<ChatConversationRecord> findConversation(String userId, String conversationId) {
        List<ChatConversationRecord> result = jdbcTemplate.query(
                """
                SELECT id, user_id, title, provider, model_name, archived,
                       created_at, updated_at
                FROM chat_conversation
                WHERE user_id = ? AND id = ? AND archived = FALSE
                """,
                this::mapConversation,
                userId,
                conversationId);
        return result.stream().findFirst();
    }

    public ChatConversationRecord createConversation(
            String userId,
            String title,
            String provider,
            String model) {
        String conversationId = uuid();
        jdbcTemplate.update(
                """
                INSERT INTO chat_conversation (
                    id, user_id, title, provider, model_name
                ) VALUES (?, ?, ?, ?, ?)
                """,
                conversationId,
                userId,
                title,
                provider,
                model);
        return findConversation(userId, conversationId)
                .orElseThrow(() -> new IllegalStateException("聊天会话创建失败"));
    }

    public void touchConversation(String conversationId, String provider, String model) {
        jdbcTemplate.update(
                """
                UPDATE chat_conversation
                SET provider = ?, model_name = ?, updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """,
                provider,
                model,
                conversationId);
    }

    public List<ChatMessageRecord> findMessages(String conversationId) {
        return jdbcTemplate.query(
                """
                SELECT id, conversation_id, role, content, provider, model_name,
                       prompt_tokens, completion_tokens, total_tokens,
                       error_code, error_message, created_at
                FROM chat_message
                WHERE conversation_id = ?
                ORDER BY created_at, id
                """,
                this::mapMessage,
                conversationId);
    }

    public List<ChatMessageRecord> findRecentMessages(String conversationId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT * FROM (
                    SELECT id, conversation_id, role, content, provider, model_name,
                           prompt_tokens, completion_tokens, total_tokens,
                           error_code, error_message, created_at
                    FROM chat_message
                    WHERE conversation_id = ?
                    ORDER BY created_at DESC, id DESC
                    LIMIT ?
                ) recent_messages
                ORDER BY created_at, id
                """,
                this::mapMessage,
                conversationId,
                limit);
    }

    public ChatMessageRecord saveMessage(
            String conversationId,
            String role,
            String content,
            String provider,
            String model,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            String errorCode,
            String errorMessage) {
        String messageId = uuid();
        jdbcTemplate.update(
                """
                INSERT INTO chat_message (
                    id, conversation_id, role, content, provider, model_name,
                    prompt_tokens, completion_tokens, total_tokens,
                    error_code, error_message
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                messageId,
                conversationId,
                role,
                content,
                provider,
                model,
                promptTokens,
                completionTokens,
                totalTokens,
                errorCode,
                errorMessage);
        return jdbcTemplate.queryForObject(
                """
                SELECT id, conversation_id, role, content, provider, model_name,
                       prompt_tokens, completion_tokens, total_tokens,
                       error_code, error_message, created_at
                FROM chat_message
                WHERE id = ?
                """,
                this::mapMessage,
                messageId);
    }

    public void recordModelCallSuccess(
            String conversationId,
            String requestMessageId,
            String responseMessageId,
            ModelChatResponse response) {
        jdbcTemplate.update(
                """
                INSERT INTO chat_model_call_record (
                    id, conversation_id, request_message_id, response_message_id,
                    provider, model_name, status, prompt_tokens, completion_tokens,
                    total_tokens, duration_ms, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, 'SUCCEEDED', ?, ?, ?, ?, CURRENT_TIMESTAMP(6))
                """,
                uuid(),
                conversationId,
                requestMessageId,
                responseMessageId,
                response.provider(),
                response.model(),
                response.promptTokens(),
                response.completionTokens(),
                response.totalTokens(),
                response.durationMs());
    }

    public void recordModelCallFailure(
            String conversationId,
            String requestMessageId,
            String provider,
            String model,
            long durationMs,
            String errorCode,
            String errorMessage) {
        jdbcTemplate.update(
                """
                INSERT INTO chat_model_call_record (
                    id, conversation_id, request_message_id, provider, model_name,
                    status, duration_ms, error_code, error_message, completed_at
                ) VALUES (?, ?, ?, ?, ?, 'FAILED', ?, ?, ?, CURRENT_TIMESTAMP(6))
                """,
                uuid(),
                conversationId,
                requestMessageId,
                provider,
                model,
                durationMs,
                errorCode,
                errorMessage);
    }

    private UiPreferenceRecord mapPreference(ResultSet resultSet, int rowNum) throws SQLException {
        return new UiPreferenceRecord(
                resultSet.getString("user_id"),
                resultSet.getString("theme_id"),
                resultSet.getString("custom_accent_color"),
                resultSet.getBoolean("sidebar_collapsed"),
                resultSet.getString("default_provider"),
                resultSet.getString("default_model"));
    }

    private ChatConversationRecord mapConversation(ResultSet resultSet, int rowNum) throws SQLException {
        return new ChatConversationRecord(
                resultSet.getString("id"),
                resultSet.getString("user_id"),
                resultSet.getString("title"),
                resultSet.getString("provider"),
                resultSet.getString("model_name"),
                resultSet.getBoolean("archived"),
                resultSet.getObject("created_at", LocalDateTime.class),
                resultSet.getObject("updated_at", LocalDateTime.class));
    }

    private ChatMessageRecord mapMessage(ResultSet resultSet, int rowNum) throws SQLException {
        return new ChatMessageRecord(
                resultSet.getString("id"),
                resultSet.getString("conversation_id"),
                resultSet.getString("role"),
                resultSet.getString("content"),
                resultSet.getString("provider"),
                resultSet.getString("model_name"),
                resultSet.getLong("prompt_tokens"),
                resultSet.getLong("completion_tokens"),
                resultSet.getLong("total_tokens"),
                resultSet.getString("error_code"),
                resultSet.getString("error_message"),
                resultSet.getObject("created_at", LocalDateTime.class));
    }

    private static String uuid() {
        return UUID.randomUUID().toString();
    }
}
