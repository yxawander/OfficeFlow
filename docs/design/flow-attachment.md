# 审批申请附件管理功能设计文档

## 概述

为 flow-service（审批服务）新增附件上传与管理功能，用户在提交申请时可上传证明材料（如请假条、加班确认等），附件存储在阿里云 OSS 中。沿用了 notice-service 的附件设计模式，采用"先上传后绑定"的两阶段提交策略。

## 数据库设计

### flow_attachment 表

```sql
CREATE TABLE IF NOT EXISTS flow_attachment (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '附件ID',
    flow_apply_id   BIGINT       NULL     COMMENT '审批单ID（上传时可为空，提交申请后绑定）',
    file_name       VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_url        VARCHAR(500) NOT NULL COMMENT 'OSS文件访问URL',
    file_size       BIGINT       DEFAULT NULL COMMENT '文件大小（字节）',
    file_type       VARCHAR(64)  DEFAULT NULL COMMENT 'MIME类型，如 application/pdf',
    oss_key         VARCHAR(512) DEFAULT NULL COMMENT 'OSS对象Key，如 flow/2026-07/uuid.pdf',
    uploaded_by     BIGINT       NOT NULL COMMENT '上传人ID',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_flow_attachment_apply (flow_apply_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批附件表';
```

### 迁移 SQL

`oss_key` 字段为本次新增，已有数据库需执行迁移：

```sql
-- docs/sql/flow-attachment-oss.sql
ALTER TABLE flow_attachment
    ADD COLUMN oss_key VARCHAR(512) DEFAULT NULL COMMENT 'OSS对象Key' AFTER file_type;
```

## 架构设计

### 附件上传流程

```
前端 → POST /api/flow/attachments/upload (multipart/form-data)
     → OssServiceImpl.upload(file, objectKey)
     → 阿里云 OSS SDK → OSS Bucket
     → 返回文件URL
     → FlowAttachmentMapper.insert() 保存记录（flow_apply_id = NULL）
     → 返回 AttachmentVO { id, fileName, fileUrl, fileSize, fileType }
```

### 附件与申请绑定（两阶段提交）

```
提交申请:
  前端: 1) POST /api/flow/attachments/upload   → 获得附件ID列表 [1, 2]
        2) POST /api/flow/applies  { ..., "attachmentIds": [1, 2] }
  后端: FlowServiceImpl.createApply()
        → flowApplyMapper.insert(apply)
        → flowAttachmentMapper.updateFlowApplyId(attachmentIds, applyId)

编辑申请 (PENDING状态):
  前端: PUT /api/flow/applies/{id}  { ..., "attachmentIds": [1, 3] }
  后端: FlowServiceImpl.updateApply()
        → flowApplyMapper.update(apply)
        → flowAttachmentMapper.deleteByApplyId(id)     // 删除旧关联
        → flowAttachmentMapper.updateFlowApplyId(attachmentIds, id)  // 绑定新附件

删除申请:
  后端: FlowServiceImpl.deleteApply()
        → flowApplyMapper.deleteById(id)
        → flowAttachmentMapper.deleteByApplyId(id)     // 物理删除附件记录
```

### 查看附件

```
获取申请详情:
  GET /api/flow/applies/{id}
  响应 FlowApplyDetailVO 中包含 attachments 字段:
  {
    "id": 1,
    "title": "年假申请",
    "attachments": [
      { "id": 1, "fileName": "请假条.pdf", "fileUrl": "https://...", "fileSize": 102400, "fileType": "application/pdf" }
    ]
  }
```

### OSS 对象 Key 命名规则

```
格式: flow/{yyyy-MM}/{uuid}{扩展名}
示例: flow/2026-07/a1b2c3d4e5f6.pdf
```

## API 端点

### 1. 上传附件

```
POST /api/flow/attachments/upload
Content-Type: multipart/form-data
Header: X-Login-User-Id: {userId}

参数:
  file: MultipartFile (必填)

响应:
{
  "code": 200,
  "data": {
    "id": 1,
    "fileName": "请假申请表.pdf",
    "fileUrl": "https://bucket.oss-cn-hangzhou.aliyuncs.com/flow/2026-07/a1b2c3.pdf",
    "fileSize": 102400,
    "fileType": "application/pdf"
  }
}
```

### 2. 删除附件

```
DELETE /api/flow/attachments/{id}
Header: X-Login-User-Id: {userId}

响应:
{
  "code": 200
}
```

说明：同时删除 OSS 上的文件和数据库记录。仅当附件尚未绑定到审批单时可删除。

## 新增/修改的文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `flow-service/pom.xml` | 修改 | 新增 `aliyun-sdk-oss:3.17.4` 依赖 |
| `flow-service/.../config/OssProperties.java` | 新增 | OSS 配置属性类（`oss.*`，从 Nacos 读取） |
| `flow-service/.../service/OssService.java` | 新增 | OSS 服务接口 |
| `flow-service/.../service/impl/OssServiceImpl.java` | 新增 | OSS 服务实现（阿里云 SDK，懒加载） |
| `flow-service/.../vo/AttachmentVO.java` | 新增 | 附件响应 VO |
| `flow-service/.../entity/FlowAttachment.java` | 修改 | 新增 `ossKey` 字段 |
| `flow-service/.../mapper/FlowAttachmentMapper.java` | 修改 | 新增 `selectById`、`deleteById`、`deleteByApplyId` |
| `flow-service/resources/mapper/FlowAttachmentMapper.xml` | 修改 | 新增 `oss_key` 列映射及新方法 SQL |
| `flow-service/.../dto/FlowApplyCreateDTO.java` | 修改 | 新增 `attachmentIds` 字段 |
| `flow-service/.../dto/FlowApplyUpdateDTO.java` | 修改 | 新增 `attachmentIds` 字段 |
| `flow-service/.../vo/FlowApplyDetailVO.java` | 修改 | 新增 `attachments` 列表 |
| `flow-service/.../service/impl/FlowServiceImpl.java` | 修改 | 注入 `FlowAttachmentMapper`，处理附件绑定/替换/删除/查询 |
| `flow-service/.../controller/FlowController.java` | 修改 | 新增上传/删除附件接口 |
| `oa-api/.../dto/FlowApplyCreateDTO.java` | 修改 | 同步 `attachmentIds` 字段 |
| `docs/sql/flow-attachment-oss.sql` | 新增 | `flow_attachment` 增加 `oss_key` 字段迁移脚本 |

## OSS 配置项

需在 Nacos 的 `flow-service.yaml` 中添加（与 notice-service 共用同一套 OSS 凭证）：

```yaml
oss:
  endpoint: oss-cn-hangzhou.aliyuncs.com
  access-key-id: <AccessKey ID>
  access-key-secret: <AccessKey Secret>
  bucket-name: officeflow
  base-url:            # （可选）CDN 或自定义域名
```

## 关键代码说明

### OssProperties — 配置绑定

```java
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "oss")
public class OssProperties {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String baseUrl;
}
```

- `@RefreshScope` 支持 Nacos 配置热更新，无需重启即可轮换 OSS 凭证
- 属性通过 `spring.config.import: optional:nacos:flow-service.yaml` 从 Nacos 加载

### OssServiceImpl — 懒加载 OSS 客户端

- 使用双重检查锁定（DCL）延迟初始化 `OSSClient`
- `getClient()` 在首次调用时创建客户端，避免启动时 Nacos 配置未就绪的问题
- `ensureClient()` 校验 OSS 是否已配置，未配置时抛出 `BusinessException`
- `@PreDestroy` 方法优雅关闭客户端连接

### FlowServiceImpl — 附件生命周期管理

| 方法 | 附件处理 |
|------|----------|
| `createApply` | 调用 `updateFlowApplyId` 将已上传的附件绑定到新建的申请 |
| `updateApply` | 若传入 `attachmentIds`，先 `deleteByApplyId` 清旧关联，再绑定新的 |
| `deleteApply` | 调用 `deleteByApplyId` 物理删除附件记录 |
| `getApplyDetail` | 查询附件列表并通过 `buildAttachmentVOs()` 转换填充到 VO |

### generateObjectKey — 文件路径生成

```java
public static String generateObjectKey(String originalFileName) {
    String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    String ext = "";
    int dotIndex = originalFileName.lastIndexOf('.');
    if (dotIndex > 0) {
        ext = originalFileName.substring(dotIndex);
    }
    return "flow/" + month + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
}
```

生成路径格式：`flow/2026-07/{uniqueId}.pdf`，按月份分组便于 OSS 生命周期管理。

## 使用方式

1. **上传附件**：调用 `POST /api/flow/attachments/upload`，获得附件 ID
2. **提交申请**：调用 `POST /api/flow/applies`，在 `attachmentIds` 中传入附件 ID 列表
3. **查看申请**：调用 `GET /api/flow/applies/{id}`，响应中 `attachments` 字段包含所有附件信息，前端可直接使用 `fileUrl` 下载或预览
4. **编辑申请**：调用 `PUT /api/flow/applies/{id}`，传入新的 `attachmentIds` 替换旧附件
5. **删除附件**：调用 `DELETE /api/flow/attachments/{id}`（仅限已上传但未绑定的情况）
6. **清理孤儿附件**：未绑定到任何审批单的附件（`flow_apply_id IS NULL`）可定期通过定时任务清理 OSS 和数据库
