package com.chatbotmvt.controller;

import com.chatbotmvt.services.ImageRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recovery")
@RequiredArgsConstructor
public class RecoveryController {

    private final ImageRecoveryService imageRecoveryService;

    @PostMapping("/images")
    public ResponseEntity<?> recoverImages() {
        int recovered = imageRecoveryService.recoverMissingImages();
        return ResponseEntity.ok(Map.of(
                "recovered", recovered,
                "message", recovered + " imágenes recuperadas y subidas a Cloudinary"
        ));
    }
}
