package org.example.bookweb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.example.bookweb.entity.Book;
import org.example.bookweb.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 馆藏助手（v0.7，PRD 2.1/2.3 锁定）：
 * - RestClient 调智谱开放平台 chat/completions，非流式（禁止 SSE，列为 v1.x 增强）；
 * - GLM_API_KEY 走环境变量；模型默认 glm-4.6，仅 GLM_MODEL 可覆盖（模型不存在 → 阻塞上报，禁止换模型）；
 * - function calling 仅两工具：search_books(keyword, category) / get_book_status(id)；
 * - 系统提示词写死约束：回答必须基于工具返回的真实馆藏，工具查不到就答「馆内没有」；
 * - key 只走环境变量，不落日志/响应（PRD 3.3）。
 */
@Slf4j
@Service
public class AssistantService {

    /** 未配置 GLM_API_KEY → 200 + data:null + message「AI 服务未配置」 */
    public static class NotConfiguredException extends RuntimeException {
        public NotConfiguredException(String message) { super(message); }
    }

    /** GLM 调用失败/超时 → 200 + data:null + 明确 message（禁止 500） */
    public static class UpstreamUnavailableException extends RuntimeException {
        public UpstreamUnavailableException(String message) { super(message); }
    }

    private static final String GLM_CHAT_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    /** 工具调用循环上限：防止模型反复 tool_call 拖死请求 */
    private static final int MAX_TOOL_ROUNDS = 3;
    /** GLM 连接超时；读取超时放宽（LLM 生成耗时），PRD 6.1 未定义覆盖变量，固定常量 */
    private static final int GLM_CONNECT_TIMEOUT_MS = 5_000;
    private static final int GLM_READ_TIMEOUT_MS = 60_000;

    /** 系统提示词（PRD v0.7 强制规范：写死馆藏约束） */
    private static final String SYSTEM_PROMPT =
            "你是图书馆馆藏助手。你的回答必须且只能基于工具返回的真实馆藏数据："
                    + "用 search_books 按关键词或分类检索图书，用 get_book_status 按 id 查询在馆/在借状态。"
                    + "工具查不到的书，一律回答「馆内没有」。禁止编造、猜测或使用馆藏之外的任何数据。";

    private final BookMapper bookMapper;
    private final String apiKey;
    private final String model;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssistantService(BookMapper bookMapper,
                            @Value("${GLM_API_KEY:}") String apiKey,
                            @Value("${GLM_MODEL:glm-4.6}") String model) {
        this.bookMapper = bookMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(GLM_CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(GLM_READ_TIMEOUT_MS);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** 单轮对话（非流式）：自动执行工具调用并回喂结果，返回最终 reply */
    public String chat(String message) {
        if (apiKey.isEmpty()) {
            throw new NotConfiguredException("AI 服务未配置");
        }
        try {
            ArrayNode messages = objectMapper.createArrayNode();
            messages.add(messageNode("system", SYSTEM_PROMPT));
            messages.add(messageNode("user", message));

            for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
                JsonNode assistantMessage = callChatCompletions(messages);
                JsonNode toolCalls = assistantMessage.path("tool_calls");
                if (!toolCalls.isArray() || toolCalls.isEmpty()) {
                    String reply = assistantMessage.path("content").asText("");
                    return reply.isBlank() ? "馆内没有相关馆藏信息" : reply;
                }
                // 回喂：assistant（含 tool_calls）+ 每个调用的 tool 结果
                messages.add(assistantMessage.deepCopy());
                for (JsonNode call : toolCalls) {
                    String toolContent = executeTool(call.path("function").path("name").asText(),
                            call.path("function").path("arguments").asText("{}"));
                    ObjectNode toolMessage = messages.addObject().put("role", "tool");
                    toolMessage.put("tool_call_id", call.path("id").asText());
                    toolMessage.put("content", toolContent);
                }
            }
            return "馆内没有相关馆藏信息";
        } catch (RestClientException e) {
            log.warn("GLM 调用失败: {}", e.getMessage());
            throw new UpstreamUnavailableException("AI 服务暂时不可用，请稍后重试");
        } catch (UpstreamUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.warn("GLM 响应解析失败: {}", e.getMessage());
            throw new UpstreamUnavailableException("AI 服务响应异常，请稍后重试");
        }
    }

    /** 请求 chat/completions（非流式），返回 choices[0].message 节点 */
    private JsonNode callChatCompletions(ArrayNode messages) throws Exception {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", model);
        request.set("messages", messages);
        request.set("tools", buildTools());
        request.put("tool_choice", "auto");
        request.put("stream", false);

        String body = restClient.post()
                .uri(GLM_CHAT_URL)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request.toString())
                .retrieve()
                .body(String.class);
        if (body == null || body.isBlank()) {
            throw new UpstreamUnavailableException("AI 服务响应为空，请稍后重试");
        }
        return objectMapper.readTree(body).path("choices").path(0).path("message");
    }

    /** function calling 工具注册：仅 search_books / get_book_status（PRD 2.3 锁定，不得增删） */
    private ArrayNode buildTools() {
        ArrayNode tools = objectMapper.createArrayNode();

        ObjectNode search = tools.addObject().put("type", "function");
        ObjectNode searchFn = search.putObject("function");
        searchFn.put("name", "search_books");
        searchFn.put("description", "按关键词（书名/作者）或分类检索馆内图书，返回命中列表");
        ObjectNode searchParams = searchFn.putObject("parameters").put("type", "object");
        ObjectNode searchProps = searchParams.putObject("properties");
        searchProps.putObject("keyword").put("type", "string").put("description", "书名或作者关键词，可空");
        searchProps.putObject("category").put("type", "string").put("description", "图书分类精确匹配，可空");

        ObjectNode status = tools.addObject().put("type", "function");
        ObjectNode statusFn = status.putObject("function");
        statusFn.put("name", "get_book_status");
        statusFn.put("description", "按 id 查询馆内某本图书的在馆/在借状态与基本信息");
        ObjectNode statusParams = statusFn.putObject("parameters").put("type", "object");
        statusParams.putArray("required").add("id");
        ObjectNode statusProps = statusParams.putObject("properties");
        statusProps.putObject("id").put("type", "integer").put("description", "图书 id");
        return tools;
    }

    /** 本地执行工具：只查真实馆藏（BookMapper），结果 JSON 字符串回喂模型 */
    private String executeTool(String name, String arguments) throws Exception {
        JsonNode args = objectMapper.readTree(arguments == null || arguments.isBlank() ? "{}" : arguments);
        switch (name) {
            case "search_books" -> {
                String keyword = args.path("keyword").asText("");
                String category = args.path("category").asText("");
                List<Book> books = bookMapper.searchForAssistant(
                        keyword == null || keyword.isBlank() ? "" : keyword,
                        category == null || category.isBlank() ? "" : category,
                        10);
                List<Map<String, Object>> result = new ArrayList<>();
                for (Book b : books) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", b.getId());
                    item.put("title", b.getTitle());
                    item.put("author", b.getAuthor());
                    item.put("category", b.getCategory());
                    item.put("borrowed", b.isBorrowed());
                    result.add(item);
                }
                return objectMapper.writeValueAsString(result);
            }
            case "get_book_status" -> {
                int id = args.path("id").asInt(-1);
                Map<String, Object> result = new LinkedHashMap<>();
                Book b = id > 0 ? bookMapper.findById(id) : null;
                if (b == null) {
                    result.put("found", false);
                } else {
                    result.put("found", true);
                    result.put("id", b.getId());
                    result.put("title", b.getTitle());
                    result.put("author", b.getAuthor());
                    result.put("category", b.getCategory());
                    result.put("borrowed", b.isBorrowed());
                    result.put("totalCopies", b.getTotalCopies());
                }
                return objectMapper.writeValueAsString(result);
            }
            default -> {
                return "{\"error\":\"unknown tool\"}";
            }
        }
    }

    private ObjectNode messageNode(String role, String content) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }
}
