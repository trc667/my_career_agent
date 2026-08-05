package com.example.aimaster.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.example.aimaster.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * 阿里云 OSS 存储服务：头像图片上传。
 * <p>
 * 设计：
 * 1) 配置全部走环境变量/application-dev.yml（不入库，避免 AK 泄露）。
 * 2) 仅允许图片类型（jpg/png/webp/gif）+ 2MB 上限，防止恶意文件与超大文件。
 * 3) key 用 UUID 生成（用户头像），AI 头像用固定 key 覆盖（保证所有人看到最新一张）。
 * 4) 未配置 OSS 时抛业务异常，前端可提示"请配置 OSS"。
 */
@Slf4j
@Service
public class OssStorageService {

    private static final long MAX_AVATAR_BYTES = 2 * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    /** AI 头像固定 key：管理员上传时覆盖，所有人读取同一 URL */
    public static final String AI_AVATAR_KEY = "ai/avatar.png";

    private final String endpoint;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String bucket;

    private volatile OSS ossClient;

    public OssStorageService(
            @Value("${app.oss.endpoint:}") String endpoint,
            @Value("${app.oss.access-key-id:}") String accessKeyId,
            @Value("${app.oss.access-key-secret:}") String accessKeySecret,
            @Value("${app.oss.bucket:}") String bucket) {
        this.endpoint = endpoint == null ? "" : endpoint.trim();
        this.accessKeyId = accessKeyId == null ? "" : accessKeyId.trim();
        this.accessKeySecret = accessKeySecret == null ? "" : accessKeySecret.trim();
        this.bucket = bucket == null ? "" : bucket.trim();
    }

    /** 是否已配置 OSS（未配置时上传接口应提示配置） */
    public boolean isConfigured() {
        return !endpoint.isEmpty() && !accessKeyId.isEmpty() && !accessKeySecret.isEmpty() && !bucket.isEmpty();
    }

    /**
     * 上传用户头像：key = avatars/{uuid}.{ext}
     *
     * @return 可公开访问的图片 URL
     */
    public String uploadUserAvatar(MultipartFile file) {
        return upload(file, "avatars/" + UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * 上传/覆盖 AI 头像：固定 key（ai/avatar.png，覆盖式），所有人读取同一 URL。
     * 注意：不走通用 upload()（会追加扩展名导致 ai/avatar.png.png 双扩展名 bug），
     * 这里直接用完整固定 key 上传。
     */
    public String uploadAiAvatar(MultipartFile file) {
        validateImage(file);
        try {
            getClient().putObject(bucket, AI_AVATAR_KEY, file.getInputStream());
            log.info("OSS AI 头像上传成功：{}（{} bytes）", AI_AVATAR_KEY, file.getSize());
            return buildUrl(AI_AVATAR_KEY);
        } catch (IOException e) {
            log.error("OSS AI 头像上传失败：{}", e.getMessage());
            throw new BusinessException("AI 头像上传失败：" + e.getMessage());
        }
    }

    /** 返回 AI 头像的公开访问 URL（无论是否上传过都返回约定地址） */
    public String getAiAvatarUrl() {
        return buildUrl(AI_AVATAR_KEY);
    }

    /** 通用上传：校验类型/大小 → 上传到 OSS → 返回 URL */
    private String upload(MultipartFile file, String keyBase) {
        validateImage(file);
        String ext = resolveExtension(file.getOriginalFilename());
        String key = keyBase + "." + ext;
        try {
            getClient().putObject(bucket, key, file.getInputStream());
            log.info("OSS 上传成功：{}（{} bytes）", key, file.getSize());
            return buildUrl(key);
        } catch (IOException e) {
            log.error("OSS 上传失败：{}", e.getMessage());
            throw new BusinessException("图片上传失败：" + e.getMessage());
        }
    }

    /** 图片类型与大小校验（仅允许 jpg/png/webp/gif，2MB 内） */
    private void validateImage(MultipartFile file) {
        if (!isConfigured()) {
            throw new BusinessException("OSS 未配置：请在 application-dev.yml 或环境变量中配置 OSS_* 后重试");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BusinessException("图片大小不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("仅支持 jpg/png/webp/gif 图片");
        }
    }

    private OSS getClient() {
        if (ossClient == null) {
            synchronized (this) {
                if (ossClient == null) {
                    ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                }
            }
        }
        return ossClient;
    }

    /** 从文件名解析扩展名，白名单校验 */
    private String resolveExtension(String filename) {
        String ext = "png";
        if (filename != null && filename.contains(".")) {
            String e = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            if (ALLOWED_EXTENSIONS.contains(e)) ext = e;
        }
        return ext;
    }

    /** 生成公网可访问 URL（bucket 需为公共读） */
    private String buildUrl(String key) {
        // endpoint 形如 oss-cn-hangzhou.aliyuncs.com，去协议后拼 https://{bucket}.{endpoint}/{key}
        String host = endpoint.replaceFirst("^https?://", "");
        return "https://" + bucket + "." + host + "/" + key;
    }
}
