package com.shop.backend.service;

import io.minio.PutObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class MediaService {
    private final MinioClient minioClient;
    private final String bucket;
    private final String publicBasePath;

    public MediaService(MinioClient minioClient,
                        @Value("${shop.minio.bucket}") String bucket,
                        @Value("${shop.minio.public-base-path}") String publicBasePath) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.publicBasePath = publicBasePath;
    }

    public String uploadProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("image file is required");
        }

        String ext = extension(file.getOriginalFilename());
        String objectName = "products/" + UUID.randomUUID() + ext;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                            .build()
            );
            return publicBasePath + "/" + bucket + "/" + objectName;
        } catch (Exception e) {
            throw new IllegalStateException("failed to upload image to MinIO", e);
        }
    }

    private String extension(String filename) {
        if (filename == null) {
            return ".bin";
        }
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) {
            return ".bin";
        }
        return filename.substring(i).toLowerCase();
    }
}
