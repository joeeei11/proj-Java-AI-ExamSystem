package com.hnust.examai.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek AI 客户端封装
 * <p>
 * 统一处理 Chat Completion API 调用、超时兜底（30s）、失败重试（最多 2 次）
 * </p>
 */
@Slf4j
@Component
public class DeepSeekClient {

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_RETRY = 2;

    @Value("${app.deepseek.api-key}")
    private String apiKey;

    @Value("${app.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${app.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${app.deepseek.max-tokens:4096}")
    private int maxTokens;

    @Value("${app.deepseek.timeout-seconds:30}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper;

    /** OkHttp 客户端（每次调用根据超时时间动态创建，复用 ConnectionPool） */
    private final OkHttpClient baseClient;

    public DeepSeekClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.baseClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .build();
    }

    /**
     * 发送 Chat Completion 请求，要求 AI 以 JSON 格式响应
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI 响应的原始文本内容
     * @throws BizException AI_TIMEOUT / AI_GENERATE_FAILED
     */
    public String chat(String systemPrompt, String userPrompt) {
        String endpoint = baseUrl + "/v1/chat/completions";

        // 构建请求体（要求 JSON_OBJECT 响应格式）
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);

        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_object");
        body.set("response_format", responseFormat);

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode sysMsg = objectMapper.createObjectNode();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);
        body.set("messages", messages);

        String requestBodyStr;
        try {
            requestBodyStr = objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new BizException(ResultCode.AI_GENERATE_FAILED);
        }

        // 重试逻辑
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                String result = doRequest(endpoint, requestBodyStr);
                log.debug("DeepSeek 调用成功（第 {} 次），响应长度 {}", attempt, result.length());
                return result;
            } catch (BizException e) {
                if (e.getCode() == ResultCode.AI_TIMEOUT.getCode()) {
                    // 超时不再重试
                    throw e;
                }
                lastException = e;
                log.warn("DeepSeek 调用失败（第 {}/{} 次）: {}", attempt, MAX_RETRY, e.getMessage());
            } catch (Exception e) {
                lastException = e;
                log.warn("DeepSeek 调用异常（第 {}/{} 次）: {}", attempt, MAX_RETRY, e.getMessage());
            }
        }

        log.error("DeepSeek 调用在 {} 次重试后仍失败", MAX_RETRY, lastException);
        throw new BizException(ResultCode.AI_GENERATE_FAILED);
    }

    /**
     * 执行单次 HTTP 请求，提取 choices[0].message.content
     */
    private String doRequest(String endpoint, String requestBodyStr) {
        OkHttpClient client = baseClient.newBuilder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(requestBodyStr, JSON_MEDIA))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("DeepSeek API 返回非 2xx 状态码: {}", response.code());
                throw new BizException(ResultCode.AI_GENERATE_FAILED,
                        "AI 服务响应异常: " + response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new BizException(ResultCode.AI_GENERATE_FAILED, "AI 响应体为空");
            }

            String rawBody = responseBody.string();
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");

            if (content.isMissingNode() || content.isNull()) {
                throw new BizException(ResultCode.AI_PARSE_FAILED, "AI 响应缺少 content 字段");
            }

            return content.asText();

        } catch (SocketTimeoutException e) {
            log.error("DeepSeek 请求超时 ({}s)", timeoutSeconds);
            throw new BizException(ResultCode.AI_TIMEOUT);
        } catch (IOException e) {
            throw new BizException(ResultCode.AI_GENERATE_FAILED, "网络请求失败: " + e.getMessage());
        }
    }
}
