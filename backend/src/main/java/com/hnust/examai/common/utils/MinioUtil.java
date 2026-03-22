package com.hnust.examai.common.utils;

import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * MinIO 文件工具类
 * 封装上传、删除、URL 获取操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Value("${app.minio.bucket}")
    private String bucket;

    /**
     * 上传 MultipartFile
     *
     * @param file      上传的文件
     * @param directory 存储目录（如 "avatars/"）
     * @return 可访问的文件 URL
     */
    public String upload(MultipartFile file, String directory) {
        String ext = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String objectName = directory + UUID.randomUUID() + ext;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return buildUrl(objectName);
        } catch (Exception e) {
            log.error("MinIO 上传失败: {}", e.getMessage(), e);
            throw new BizException(ResultCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 删除对象（忽略失败）
     *
     * @param objectName 对象路径
     */
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO 删除失败（忽略）: {}", e.getMessage());
        }
    }

    /**
     * 从完整 URL 中提取对象路径
     *
     * @param url 完整 URL（如 http://localhost:9000/exam-ai/avatars/xxx.jpg）
     * @return 对象路径（如 avatars/xxx.jpg）
     */
    public String extractObjectName(String url) {
        String prefix = endpoint + "/" + bucket + "/";
        if (url != null && url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        return null;
    }

    /**
     * 构建对象访问 URL
     */
    public String buildUrl(String objectName) {
        return endpoint + "/" + bucket + "/" + objectName;
    }
}
