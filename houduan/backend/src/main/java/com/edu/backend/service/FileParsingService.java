package com.edu.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class FileParsingService {

    private static final int MIN_MEANINGFUL_TEXT_LENGTH = 20;

    /**
     * 解析上传的文件，返回文本内容（MultipartFile 版本）
     */
    public String parseFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名为空");
        }
        return parseFile(file.getBytes(), fileName);
    }

    /**
     * 解析文件字节内容（byte[] 版本，避免重复读流）
     */
    public String parseFile(byte[] bytes, String fileName) throws Exception {
        if (fileName == null) {
            throw new IllegalArgumentException("文件名为空");
        }
        String lowerName = fileName.toLowerCase();

        if (lowerName.endsWith(".pdf")) {
            return parsePdf(bytes);
        } else if (lowerName.endsWith(".docx")) {
            return parseDocx(bytes);
        } else if (lowerName.endsWith(".doc")) {
            return parseDoc(bytes);
        } else if (lowerName.endsWith(".txt")) {
            return parseTxt(bytes);
        } else if (lowerName.endsWith(".md")) {
            return parseMarkdown(bytes);
        } else {
            throw new IllegalArgumentException("不支持的文件类型: " + fileName);
        }
    }

    /**
     * 解析 PDF —— Apache PDFBox
     */
    private String parsePdf(byte[] bytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(doc);
            String trimmed = text != null ? text.trim() : "";
            if (trimmed.length() < MIN_MEANINGFUL_TEXT_LENGTH) {
                throw new IllegalArgumentException("PDF无可提取的文本层（可能是扫描件或图片型PDF），请上传包含可选文本的PDF");
            }
            return trimmed;
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("password")) {
                throw new IllegalArgumentException("PDF文件已加密，请解密后重新上传");
            }
            throw new IllegalArgumentException("PDF解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析 .docx —— Apache POI XWPF
     */
    private String parseDocx(byte[] bytes) throws Exception {
        try (InputStream in = new ByteArrayInputStream(bytes);
             XWPFDocument doc = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            String trimmed = text != null ? text.trim() : "";
            if (trimmed.length() < MIN_MEANINGFUL_TEXT_LENGTH) {
                throw new IllegalArgumentException("DOCX文件未提取到有效文本内容");
            }
            return trimmed;
        }
    }

    /**
     * 解析老式 .doc —— Apache POI HWPF
     */
    private String parseDoc(byte[] bytes) throws Exception {
        try (InputStream in = new ByteArrayInputStream(bytes);
             HWPFDocument doc = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(doc)) {
            String text = extractor.getText();
            String trimmed = text != null ? text.trim() : "";
            if (trimmed.length() < MIN_MEANINGFUL_TEXT_LENGTH) {
                throw new IllegalArgumentException("DOC文件未提取到有效文本内容");
            }
            return trimmed;
        }
    }

    /**
     * 解析 TXT 文件
     */
    private String parseTxt(byte[] bytes) throws Exception {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 解析 Markdown 文件
     */
    private String parseMarkdown(byte[] bytes) throws Exception {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 获取文件大小（人类可读格式）
     */
    public String getReadableFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 验证文件类型
     */
    public boolean isValidFileType(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) return false;

        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".pdf") ||
               lowerName.endsWith(".docx") ||
               lowerName.endsWith(".doc") ||
               lowerName.endsWith(".txt") ||
               lowerName.endsWith(".md");
    }

    /**
     * 验证文件大小（最大10MB）
     */
    public boolean isValidFileSize(MultipartFile file) {
        return file.getSize() <= 10 * 1024 * 1024;
    }
}
