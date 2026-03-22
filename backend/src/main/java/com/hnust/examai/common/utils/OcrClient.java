package com.hnust.examai.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * OCR 微服务客户端
 * <p>
 * 调用 PaddleOCR Flask 微服务（POST /ocr），超时 30s
 * 失败时抛出 {@link ResultCode#OCR_FAILED}，而非 500
 * </p>
 */
@Slf4j
@Component
public class OcrClient {

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 30;

    @Value("${app.ocr.service-url:http://localhost:5001}")
    private String serviceUrl;

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public OcrClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 识别图片文字
     *
     * @param imageBase64 Base64 编码的图片（可含 data URI 前缀，如 data:image/png;base64,...）
     * @return 识别出的全文文本
     * @throws BizException ResultCode.OCR_FAILED
     */
    public String recognize(String imageBase64) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("imageBase64", imageBase64);

        String requestBodyStr;
        try {
            requestBodyStr = objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new BizException(ResultCode.OCR_FAILED, "OCR 请求序列化失败");
        }

        Request request = new Request.Builder()
                .url(serviceUrl + "/ocr")
                .post(RequestBody.create(requestBodyStr, JSON_MEDIA))
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("OCR 微服务返回非 2xx 状态码: {}", response.code());
                throw new BizException(ResultCode.OCR_FAILED, "OCR 服务响应异常: " + response.code());
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new BizException(ResultCode.OCR_FAILED, "OCR 服务响应体为空");
            }

            JsonNode root = objectMapper.readTree(responseBody.string());

            // 检查 OCR 服务返回的业务错误
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                log.error("OCR 服务返回错误: {}", errorNode.asText());
                throw new BizException(ResultCode.OCR_FAILED, errorNode.asText());
            }

            String text = root.path("text").asText("");
            log.info("OCR 识别完成，文本长度 {}", text.length());
            return text;

        } catch (BizException e) {
            throw e;
        } catch (SocketTimeoutException e) {
            log.error("OCR 微服务请求超时 ({}s)", TIMEOUT_SECONDS);
            throw new BizException(ResultCode.OCR_FAILED, "OCR 服务请求超时，请稍后重试");
        } catch (IOException e) {
            log.error("OCR 微服务调用失败: {}", e.getMessage(), e);
            throw new BizException(ResultCode.OCR_FAILED, "OCR 服务网络异常: " + e.getMessage());
        }
    }
}
