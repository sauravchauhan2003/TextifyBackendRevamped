package com.example.demo.ProfilePhoto;

import com.example.demo.Authentication.UserModel;
import com.example.demo.Authentication.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.*;

@Service
public class ProfilePhotoService {

    private static final String BASE_UPLOAD_DIR = "uploads/profile/";

    @Autowired
    private UserRepository userRepository;

    @Async // or just @Async if no custom executor
    public void processProfilePhoto(byte[] fileBytes, Integer userId) {

        try {
            Path userDir = Paths.get(BASE_UPLOAD_DIR, userId.toString());
            Files.createDirectories(userDir);

            Path fullPath = userDir.resolve("full.jpg");
            Path previewPath = userDir.resolve("preview.jpg");

            // ✅ Save original file
            Files.write(fullPath, fileBytes);

            // ✅ Decode from memory (faster than reading from disk again)
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (original == null) return;

            int targetWidth = 300;
            int targetHeight = (original.getHeight() * targetWidth) / original.getWidth();

            BufferedImage preview = new BufferedImage(
                    targetWidth,
                    targetHeight,
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g = preview.createGraphics();
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
            g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
            g.dispose();

            ImageIO.write(preview, "jpg", previewPath.toFile());

            // ✅ Update DB
            UserModel user = userRepository.findById(userId).orElseThrow();
            user.setProfilePhotoPath(fullPath.toString());
            user.setProfilePhotoPreviewPath(previewPath.toString());
            userRepository.save(user);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}