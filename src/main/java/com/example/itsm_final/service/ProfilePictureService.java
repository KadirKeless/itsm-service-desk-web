package com.example.itsm_final.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Masaustu ImageUtils + UserService.updateProfilePicture ile ayni mantik:
 * kare kirp, max 800px, PNG olarak assets-avatars/avatar_{id}.png kaydet.
 */
@Service
public class ProfilePictureService {

    private static final int MAX_DIMENSION = 800;
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path avatarDir;

    public ProfilePictureService(@Value("${itsm.avatar-dir:assets-avatars}") String avatarDirName) {
        this.avatarDir = Paths.get(avatarDirName).toAbsolutePath().normalize();
        try {
            Files.createDirectories(avatarDir);
        } catch (IOException e) {
            throw new IllegalStateException("Avatar klasoru olusturulamadi: " + avatarDir, e);
        }
    }

    public Path getAvatarDirectory() {
        return avatarDir;
    }

    /** DB'de saklanan yol (masaustu ile ayni format). */
    public String buildRelativePath(Integer userId) {
        return "assets-avatars/avatar_" + userId + ".png";
    }

    public byte[] processUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new UserService.BusinessException("Lutfen bir resim dosyasi seciniz.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new UserService.BusinessException(
                    "Profil resmi boyutu 5 MB'i gecemez! Lutfen daha kucuk bir resim secin.");
        }
        String contentType = file.getContentType();
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if ((contentType == null || !contentType.startsWith("image/"))
                && !name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".png")) {
            throw new UserService.BusinessException("Yalnizca JPG veya PNG resim yukleyebilirsiniz.");
        }

        BufferedImage img = ImageIO.read(file.getInputStream());
        if (img == null) {
            throw new UserService.BusinessException("Resim okunamadi veya desteklenmeyen format!");
        }

        int minDim = Math.min(img.getWidth(), img.getHeight());
        int cx = (img.getWidth() - minDim) / 2;
        int cy = (img.getHeight() - minDim) / 2;
        BufferedImage cropped = img.getSubimage(cx, cy, minDim, minDim);

        BufferedImage scaled = cropped;
        if (minDim > MAX_DIMENSION) {
            scaled = resize(cropped, MAX_DIMENSION);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(scaled, "png", baos);
        return baos.toByteArray();
    }

    public String saveForUser(Integer userId, MultipartFile file) throws IOException {
        byte[] bytes = processUpload(file);
        Path target = avatarDir.resolve("avatar_" + userId + ".png");
        Files.write(target, bytes);
        return buildRelativePath(userId);
    }

    public void deleteFile(Integer userId, String storedPath) {
        Path file = resolveStoredPath(storedPath);
        if (file != null) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ignored) {
                // masaustu ile ayni: sessizce devam
            }
        }
        Path defaultFile = avatarDir.resolve("avatar_" + userId + ".png");
        try {
            Files.deleteIfExists(defaultFile);
        } catch (IOException ignored) {
        }
    }

    public Path resolveStoredPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }
        String normalized = storedPath.replace('\\', '/');
        if (normalized.startsWith("assets-avatars/")) {
            return avatarDir.resolve(normalized.substring("assets-avatars/".length())).normalize();
        }
        return avatarDir.resolve(Paths.get(normalized).getFileName()).normalize();
    }

    public boolean fileExists(String storedPath) {
        Path p = resolveStoredPath(storedPath);
        return p != null && Files.isRegularFile(p);
    }

    private BufferedImage resize(BufferedImage src, int target) {
        BufferedImage result = new BufferedImage(target, target, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(src, 0, 0, target, target, null);
        g2d.dispose();
        return result;
    }
}
