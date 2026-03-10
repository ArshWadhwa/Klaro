package org.example.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    /**
     * Upload PDF to Cloudinary
     * Returns Map with: url, public_id, secure_url, format, pages, bytes
     */
    public Map<String, Object> uploadPDF(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "resource_type", "raw",  // For PDFs
                        "folder", "klaro_documents",
                        "use_filename", true,
                        "unique_filename", true
                ));

        return uploadResult;
    }

    /**
     * Delete file from Cloudinary
     */
    public void deleteFile(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("resource_type", "raw"));
    }

    /**
     * Get PDF URL
     */
    public String getFileUrl(String publicId) {
        return cloudinary.url()
                .resourceType("raw")
                .generate(publicId);
    }
}