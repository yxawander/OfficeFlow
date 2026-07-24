package com.officeflow.notice.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {

    /**
     * 上传文件到OSS
     * @param file       上传的文件
     * @param objectKey  OSS对象Key（不含前缀），如 "notice/2024/01/uuid.pdf"
     * @return 文件访问URL
     */
    String upload(MultipartFile file, String objectKey);

    /**
     * 根据OSS Key删除文件
     */
    void delete(String objectKey);

    /**
     * 获取文件的临时访问URL（私有Bucket时使用）
     * @param objectKey OSS对象Key
     * @param expireSeconds 过期秒数
     * @return 带签名的临时URL
     */
    String getPresignedUrl(String objectKey, long expireSeconds);

    /**
     * 获取文件访问URL（公开Bucket）
     */
    String getFileUrl(String objectKey);

    /**
     * 从OSS下载文件并写入输出流
     * @param objectKey    OSS对象Key
     * @param outputStream 目标输出流
     */
    void downloadToStream(String objectKey, java.io.OutputStream outputStream);
}
