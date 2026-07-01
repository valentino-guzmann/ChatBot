package com.chatbotmvt.services;

import com.chatbotmvt.entity.MensajeLog;
import com.chatbotmvt.repository.MensajeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageRecoveryService {

    private final MensajeLogRepository mensajeLogRepository;
    private final WhatsappService whatsappService;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public int recoverMissingImages() {
        // Buscar mensajes con mediaId pero sin URL de Cloudinary (local o nula)
        List<MensajeLog> missing = mensajeLogRepository.findRecoverableImages();
        log.info("🔍 Encontradas {} imágenes para recuperar", missing.size());

        int recovered = 0;
        for (MensajeLog msg : missing) {
            try {
                // Obtener info del media desde WhatsApp
                WhatsappService.MediaInfo info = whatsappService.getMediaInfo(msg.getMediaId());
                if (info == null || info.url() == null) {
                    log.warn("⚠️ MediaId {} no disponible en WhatsApp (expirado?)", msg.getMediaId());
                    continue;
                }

                // Descargar bytes
                byte[] bytes = whatsappService.downloadMediaBytes(info.url());
                if (bytes == null || bytes.length == 0) {
                    log.warn("⚠️ No se pudieron descargar bytes para mediaId {}", msg.getMediaId());
                    continue;
                }

                // Generar nombre de archivo
                String extension = getExtensionFromMimeType(info.mimeType());
                String filename = msg.getPhone() + "_" + java.util.UUID.randomUUID() + extension;

                // Subir a Cloudinary
                String cloudinaryUrl = cloudinaryService.uploadImage(bytes, filename);
                if (cloudinaryUrl == null) {
                    log.warn("⚠️ Cloudinary no configurado, no se pudo subir la imagen");
                    continue;
                }

                // Actualizar la base de datos
                msg.setMediaUrl(cloudinaryUrl);
                mensajeLogRepository.save(msg);
                recovered++;
                log.info("✅ Imagen recuperada: {} -> {}", msg.getMediaId(), cloudinaryUrl);

            } catch (Exception e) {
                log.error("❌ Error recuperando imagen para mediaId {}: {}", msg.getMediaId(), e.getMessage());
            }
        }

        log.info("🎉 Recuperación completada: {}/{} imágenes recuperadas", recovered, missing.size());
        return recovered;
    }

    private String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return ".jpg";
        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
