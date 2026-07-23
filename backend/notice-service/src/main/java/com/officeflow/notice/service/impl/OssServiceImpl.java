package com.officeflow.notice.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.officeflow.common.exception.BusinessException;
import com.officeflow.notice.service.OssService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class OssServiceImpl implements OssService {

    @Value("${oss.endpoint:}")
    private String endpoint;

    @Value("${oss.access-key-id:}")
    private String accessKeyId;

    @Value("${oss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${oss.bucket-name:}")
    private String bucketName;

    @Value("${oss.base-url:}")
    private String baseUrl;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        if (endpoint.isEmpty() || accessKeyId.isEmpty() || accessKeySecret.isEmpty()) {
            log.warn("OSS config not provided, OSS client disabled. Configure oss.* in Nacos or environment variables.");
            return;
        }
        ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        log.info("OSS client initialized, endpoint={}, bucket={}", endpoint, bucketName);
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    private void ensureClient() {
        if (ossClient == null) {
            throw new BusinessException("OSS未配置，无法上传文件。请在Nacos中配置oss.*参数。");
        }
    }

    @Override
    public String upload(MultipartFile file, String objectKey) {
        ensureClient();
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, inputStream, metadata);
            ossClient.putObject(request);
            log.info("File uploaded to OSS: bucket={}, key={}, size={}", bucketName, objectKey, file.getSize());
            return getFileUrl(objectKey);
        } catch (Exception e) {
            log.error("Failed to upload file to OSS: key={}", objectKey, e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public void delete(String objectKey) {
        if (ossClient == null) {
            return;
        }
        try {
            ossClient.deleteObject(bucketName, objectKey);
            log.info("File deleted from OSS: bucket={}, key={}", bucketName, objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete file from OSS: key={}", objectKey, e);
        }
    }

    @Override
    public String getPresignedUrl(String objectKey, long expireSeconds) {
        ensureClient();
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
            URL url = ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: key={}", objectKey, e);
            throw new BusinessException("获取文件访问链接失败");
        }
    }

    @Override
    public String getFileUrl(String objectKey) {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            // 自定义域名或CDN
            return baseUrl.endsWith("/") ? baseUrl + objectKey : baseUrl + "/" + objectKey;
        }
        // OSS默认外网域名
        return String.format("https://%s.%s/%s", bucketName, endpoint, objectKey);
    }

    /**
     * 生成OSS对象Key：notice/{yyyy-MM}/{uuid}_{originalFileName}
     */
    public static String generateObjectKey(String originalFileName) {
        String month = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        String ext = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            ext = originalFileName.substring(dotIndex);
        }
        return "notice/" + month + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
    }
}
