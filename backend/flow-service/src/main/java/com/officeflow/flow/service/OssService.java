package com.officeflow.flow.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {

    String upload(MultipartFile file, String objectKey);

    void delete(String objectKey);

    String getPresignedUrl(String objectKey, long expireSeconds);

    String getFileUrl(String objectKey);

    void downloadToStream(String objectKey, java.io.OutputStream outputStream);
}
