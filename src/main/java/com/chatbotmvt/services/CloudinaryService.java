package com.chatbotmvt.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {

        if (cloudName == null || cloudName.isBlank() ||
            apiKey == null || apiKey.isBlank() ||
            apiSecret == null || apiSecret.isBlank()) {
            log.warn("☁️ Cloudinary no configurado. Las imágenes se guardarán localmente.");
            this.cloudinary = null;
        } else {
            this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
            log.info("☁️ Cloudinary configurado: {}", cloudName);
        }
    }

    public String uploadImage(byte[] imageBytes, String filename) {
        if (cloudinary == null) {
            return null; // Fallback a almacenamiento local
        }
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    imageBytes,
                    ObjectUtils.asMap(
                            "public_id", "whatsapp_images/" + filename.replace(".jpg", "").replace(".png", "").replace(".jpeg", ""),
                            "folder", "whatsapp_images",
                            "overwrite", false
                    )
            );
            String url = (String) uploadResult.get("secure_url");
            log.info("☁️ Imagen subida a Cloudinary: {}", url);
            return url;
        } catch (IOException e) {
            log.error("❌ Error subiendo imagen a Cloudinary: {}. Fallback a local.", e.getMessage());
            return null;
        }
    }
}
