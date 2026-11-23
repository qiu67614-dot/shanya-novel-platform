package com.sdjz.novel.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import java.util.Date;

@Service
public class OssCoverStorage {
    private static final Logger log = LoggerFactory.getLogger(OssCoverStorage.class);
    private static final long MAX_BYTES = 2 * 1024 * 1024;

    @Value("${aliyun.oss.endpoint:}") private String endpoint;
    @Value("${aliyun.oss.bucket:}") private String bucket;
    @Value("${aliyun.oss.access-key-id:}") private String accessKeyId;
    @Value("${aliyun.oss.access-key-secret:}") private String accessKeySecret;
    @Value("${aliyun.oss.public-base-url:}") private String publicBaseUrl;

    public String upload(MultipartFile file) {
        if (file == null) return null;
        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "封面须为非空且不超过 2 MB 的图片");
        }
        if (blank(endpoint) || blank(bucket) || blank(accessKeyId) || blank(accessKeySecret)
                || blank(publicBaseUrl) || !publicBaseUrl.startsWith("https://")) {
            throw new BusinessException("B0000", "封面云存储尚未配置");
        }
        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (IOException ex) { throw new BusinessException("B0000", "读取封面失败"); }
        String format = imageFormat(bytes);
        String key = "covers/private/" + UUID.randomUUID() + ("png".equals(format) ? ".png" : ".jpg");
        String url = publicBaseUrl.replaceAll("/+$", "") + "/" + key;
        if (url.length() > 255) throw new BusinessException("B0000", "封面访问域名过长");

        OSS client;
        try { client = client(); }
        catch (RuntimeException ex) { throw new BusinessException("B0000", "封面云存储连接失败"); }
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("png".equals(format) ? "image/png" : "image/jpeg");
            metadata.setContentLength(bytes.length);
            metadata.setCacheControl("private, max-age=600");
            client.putObject(new PutObjectRequest(bucket, key, new ByteArrayInputStream(bytes), metadata));
            client.setObjectAcl(bucket, key, CannedAccessControlList.Private);
        } catch (RuntimeException ex) {
            try { client.deleteObject(bucket, key); }
            catch (RuntimeException cleanupEx) { log.warn("Unable to clean private cover {}", key, cleanupEx); }
            log.warn("OSS cover upload failed", ex);
            throw new BusinessException("B0000", "封面上传失败，请稍后重试");
        } finally { client.shutdown(); }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) deleteQuietly(key);
                }
            });
        }
        return url;
    }

    public String signedUrl(String storedUrl) {
        if (blank(storedUrl)) return null;
        String key = privateKey(storedUrl);
        if (key == null) return storedUrl;
        if (blank(endpoint) || blank(bucket) || blank(accessKeyId) || blank(accessKeySecret)) return null;
        OSS client = client();
        try { return client.generatePresignedUrl(bucket, key, new Date(System.currentTimeMillis() + 10 * 60_000)).toString(); }
        finally { client.shutdown(); }
    }

    public String publish(String storedUrl) {
        if (blank(storedUrl)) return null;
        String sourceKey = privateKey(storedUrl);
        if (sourceKey == null) throw new BusinessException(ErrorCode.PARAM_ERROR, "投稿封面来源不合法");
        String publicKey = sourceKey.replaceFirst("^covers/private/", "covers/public/");
        OSS client = client();
        try {
            client.copyObject(bucket, sourceKey, bucket, publicKey);
            client.setObjectAcl(bucket, publicKey, CannedAccessControlList.PublicRead);
        }
        catch (RuntimeException ex) {
            try { client.deleteObject(bucket, publicKey); }
            catch (RuntimeException cleanupEx) { log.warn("Unable to clean unpublished cover {}", publicKey, cleanupEx); }
            log.warn("OSS cover publication failed", ex);
            throw new BusinessException("B0000", "发布封面失败，请稍后重试");
        } finally { client.shutdown(); }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) deleteQuietly(publicKey);
                }
            });
        }
        return publicBaseUrl.replaceAll("/+$", "") + "/" + publicKey;
    }

    private String privateKey(String url) {
        if (blank(publicBaseUrl)) return null;
        String prefix = publicBaseUrl.replaceAll("/+$", "") + "/covers/private/";
        if (!url.startsWith(prefix)) return null;
        String fileName = url.substring(prefix.length());
        if (!fileName.matches("[0-9a-fA-F-]{36}\\.(png|jpg)")) return null;
        return "covers/private/" + fileName;
    }

    private String imageFormat(byte[] bytes) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (stream == null) throw new IOException("No image stream");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IOException("Unsupported image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                String format = reader.getFormatName().toLowerCase();
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (!("jpeg".equals(format) || "png".equals(format))
                        || width < 100 || height < 100 || width > 4000 || height > 4000) {
                    throw new IOException("Invalid image dimensions or format");
                }
                reader.read(0);
                return format;
            } finally { reader.dispose(); }
        } catch (IOException | RuntimeException ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "封面仅支持 100-4000 像素的 JPG 或 PNG 图片");
        }
    }

    private void deleteQuietly(String key) {
        OSS client = client();
        try { client.deleteObject(bucket, key); }
        catch (RuntimeException ex) { log.warn("Unable to clean rolled-back cover {}", key, ex); }
        finally { client.shutdown(); }
    }

    private OSS client() { return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
