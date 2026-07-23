package com.officeflow.notice.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.officeflow.common.exception.BusinessException;
import com.officeflow.notice.config.OssProperties;
import com.officeflow.notice.service.OssService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class OssServiceImpl implements OssService {

    private final OssProperties ossProperties;
    private volatile OSS ossClient;
    private volatile boolean initialized;

    public OssServiceImpl(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    private OSS getClient() {
        if (initialized) {
            return ossClient;
        }
        synchronized (this) {
            if (initialized) {
                return ossClient;
            }
            String endpoint = ossProperties.getEndpoint();
            String ak = ossProperties.getAccessKeyId();
            String sk = ossProperties.getAccessKeySecret();
            log.info("Initializing OSS client — endpoint={}, accessKeyId={}, bucket={}",
                    endpoint, maskSecret(ak), ossProperties.getBucketName());
            if (endpoint == null || endpoint.isEmpty() || ak == null || ak.isEmpty() || sk == null || sk.isEmpty()) {
                log.warn("OSS credentials not configured, OSS client disabled.");
                initialized = true;
                return null;
            }
            ossClient = new OSSClientBuilder().build(endpoint, ak, sk);
            initialized = true;
            log.info("OSS client initialized successfully, bucket={}", ossProperties.getBucketName());
            return ossClient;
        }
    }

    private void ensureClient() {
        if (getClient() == null) {
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
            PutObjectRequest request = new PutObjectRequest(ossProperties.getBucketName(), objectKey, inputStream, metadata);
            ossClient.putObject(request);
            log.info("File uploaded to OSS: bucket={}, key={}, size={}", ossProperties.getBucketName(), objectKey, file.getSize());
            return getFileUrl(objectKey);
        } catch (Exception e) {
            log.error("Failed to upload file to OSS: key={}", objectKey, e);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public void delete(String objectKey) {
        OSS client = getClient();
        if (client == null) {
            return;
        }
        try {
            client.deleteObject(ossProperties.getBucketName(), objectKey);
            log.info("File deleted from OSS: bucket={}, key={}", ossProperties.getBucketName(), objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete file from OSS: key={}", objectKey, e);
        }
    }

    @Override
    public String getPresignedUrl(String objectKey, long expireSeconds) {
        ensureClient();
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
            URL url = ossClient.generatePresignedUrl(ossProperties.getBucketName(), objectKey, expiration);
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: key={}", objectKey, e);
            throw new BusinessException("获取文件访问链接失败");
        }
    }

    @Override
    public String getFileUrl(String objectKey) {
        String baseUrl = ossProperties.getBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl.endsWith("/") ? baseUrl + objectKey : baseUrl + "/" + objectKey;
        }
        return String.format("https://%s.%s/%s", ossProperties.getBucketName(), ossProperties.getEndpoint(), objectKey);
    }

    public static String generateObjectKey(String originalFileName) {
        String month = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        String ext = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            ext = originalFileName.substring(dotIndex);
        }
        return "notice/" + month + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
    }

    private static String maskSecret(String s) {
        if (s == null || s.length() <= 4) return "****";
        return s.substring(0, 4) + "****";
    }
}
