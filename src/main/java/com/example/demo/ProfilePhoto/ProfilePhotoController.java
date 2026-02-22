package com.example.demo.ProfilePhoto;

import com.example.demo.Authentication.UserModel;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;

@RestController
@RequestMapping("/api/profile")
public class ProfilePhotoController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final String BASE_UPLOAD_DIR = "uploads/profile/";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Autowired
    private ProfilePhotoService profilePhotoService;

    @Autowired
    private RedisTemplate<String, byte[]> redisTemplate;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(BASE_UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory", e);
        }
    }

    // ================================
    // 🚀 ASYNC UPLOAD
    // ================================
    @PostMapping("/upload")
    public ResponseEntity<?> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body("Max 5MB allowed");
        }

        try {
            UserModel user = (UserModel) authentication.getPrincipal();
            Integer userId = user.getId();

            byte[] fileBytes = file.getBytes();

            // 🔥 Invalidate cache when uploading new photo
            redisTemplate.delete("profile:preview:" + userId);
            redisTemplate.delete("profile:full:" + userId);

            profilePhotoService.processProfilePhoto(fileBytes, userId);

            return ResponseEntity.accepted().body("Upload started");

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Upload failed");
        }
    }

    // ================================
    // 📸 GET PROFILE PHOTO (WITH REDIS)
    // ================================
    @GetMapping("/photo/{userId}")
    public ResponseEntity<Resource> getProfilePhoto(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "preview") String type
    ) {

        try {

            String cacheKey = "profile:" + type.toLowerCase() + ":" + userId;

            // 1️⃣ Try Redis
            byte[] cachedImage = redisTemplate.opsForValue().get(cacheKey);

            if (cachedImage != null) {
                return ResponseEntity.ok()
                        .header("Content-Type", "image/jpeg")
                        .header("Cache-Control", "public, max-age=86400")
                        .body(new ByteArrayResource(cachedImage));
            }

            // 2️⃣ Load from disk
            Path imagePath;

            if ("full".equalsIgnoreCase(type)) {
                imagePath = Paths.get(BASE_UPLOAD_DIR, userId.toString(), "full.jpg");
            } else {
                imagePath = Paths.get(BASE_UPLOAD_DIR, userId.toString(), "preview.jpg");
            }

            if (!Files.exists(imagePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);

            // 3️⃣ Store in Redis
            redisTemplate.opsForValue().set(cacheKey, imageBytes, CACHE_TTL);

            return ResponseEntity.ok()
                    .header("Content-Type", "image/jpeg")
                    .header("Cache-Control", "public, max-age=86400")
                    .body(new ByteArrayResource(imageBytes));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}