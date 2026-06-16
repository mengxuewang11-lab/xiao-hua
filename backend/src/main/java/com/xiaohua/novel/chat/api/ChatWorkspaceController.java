package com.xiaohua.novel.chat.api;

import java.net.URI;
import java.util.List;

import com.xiaohua.novel.chat.application.ChatWorkspaceService;
import com.xiaohua.novel.chat.model.ChatConversationRecord;
import com.xiaohua.novel.shared.api.ApiPaths;
import com.xiaohua.novel.shared.config.OpenApiConfig;
import com.xiaohua.novel.shared.security.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.CHAT)
@Tag(name = "Web 对话工作台", description = "用户 Web 首页的主题偏好、侧边栏状态、模型切换、聊天会话和消息接口")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ChatWorkspaceController {

    private final ChatWorkspaceService chatWorkspaceService;

    public ChatWorkspaceController(ChatWorkspaceService chatWorkspaceService) {
        this.chatWorkspaceService = chatWorkspaceService;
    }

    @GetMapping("/models")
    @Operation(
            summary = "查看可切换的大模型",
            description = "返回前端对话框可以选择的模型列表，包括智谱免费模型和 Kimi 模型，并标记后端是否已配置 API Key。")
    @ApiResponse(responseCode = "200", description = "模型列表查询成功")
    public List<ModelOptionResponse> models() {
        return chatWorkspaceService.modelOptions().stream()
                .map(ModelOptionResponse::from)
                .toList();
    }

    @GetMapping("/preferences")
    @Operation(
            summary = "读取 Web 工作台偏好",
            description = "返回当前本地用户保存的主题、主题色、侧边栏折叠状态和默认模型，避免刷新页面后丢失设置。")
    @ApiResponse(responseCode = "200", description = "偏好读取成功")
    public UiPreferenceResponse preferences() {
        return UiPreferenceResponse.from(chatWorkspaceService.getPreference());
    }

    @PutMapping("/preferences")
    @Operation(
            summary = "保存 Web 工作台偏好",
            description = "保存主题、主题色、侧边栏折叠状态和默认模型。前端每次切换主题或模型后可以调用该接口持久化。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "偏好保存成功"),
        @ApiResponse(
                responseCode = "400",
                description = "请求参数不合法",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public UiPreferenceResponse savePreferences(
            @Valid @RequestBody UpdateUiPreferenceRequest request) {
        return UiPreferenceResponse.from(chatWorkspaceService.updatePreference(request));
    }

    @GetMapping("/conversations")
    @Operation(
            summary = "查看最近聊天会话",
            description = "返回当前本地用户最近的未归档聊天会话，用于左侧侧边栏展示对话历史。")
    @ApiResponse(responseCode = "200", description = "会话列表查询成功")
    public List<ConversationSummaryResponse> conversations() {
        return chatWorkspaceService.conversations().stream()
                .map(ConversationSummaryResponse::from)
                .toList();
    }

    @PostMapping("/conversations")
    @Operation(
            summary = "新建聊天会话",
            description = "创建一个空会话，模型默认使用当前偏好设置；也可以在请求里指定本会话的供应商和模型。")
    @ApiResponse(responseCode = "201", description = "会话创建成功")
    public ResponseEntity<ConversationSummaryResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        ChatConversationRecord conversation = chatWorkspaceService.createConversation(request);
        return ResponseEntity
                .created(URI.create("/api/chat/conversations/" + conversation.id()))
                .body(ConversationSummaryResponse.from(conversation));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(
            summary = "查看聊天会话详情",
            description = "返回会话摘要和所有消息，用于页面刷新后恢复完整聊天记录。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "会话详情查询成功"),
        @ApiResponse(
                responseCode = "404",
                description = "会话不存在",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ConversationDetailResponse conversation(
            @PathVariable String conversationId) {
        return ConversationDetailResponse.from(
                chatWorkspaceService.getConversation(conversationId),
                chatWorkspaceService.messages(conversationId));
    }

    @PostMapping("/messages")
    @Operation(
            summary = "发送消息并调用大模型",
            description = """
                    保存用户输入，读取当前会话历史，按请求中的模型供应商与模型编码调用真实大模型，
                    然后保存助手回复并返回。conversationId 为空时会自动创建新会话。
                    """)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "消息发送成功，模型回复已保存"),
        @ApiResponse(
                responseCode = "400",
                description = "请求参数不合法或模型未配置",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "502",
                description = "模型供应商接口调用失败",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public SendChatMessageResponse sendMessage(
            @Valid @RequestBody SendChatMessageRequest request) {
        ChatWorkspaceService.SendResult result = chatWorkspaceService.sendMessage(request);
        return SendChatMessageResponse.from(
                result.conversation(),
                result.userMessage(),
                result.assistantMessage());
    }
}
