package com.edu.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileParsingService {
    
    /**
     * 解析上传的文件，返回文本内容
     */
    public String parseFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        String content;
        
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            content = parsePdf(file);
        } else if (fileName != null && (fileName.toLowerCase().endsWith(".docx") || fileName.toLowerCase().endsWith(".doc"))) {
            content = parseDocx(file);
        } else if (fileName != null && fileName.toLowerCase().endsWith(".txt")) {
            content = parseTxt(file);
        } else if (fileName != null && fileName.toLowerCase().endsWith(".md")) {
            content = parseMarkdown(file);
        } else {
            throw new IllegalArgumentException("不支持的文件类型: " + fileName);
        }
        
        return content;
    }
    
    /**
     * 解析PDF文件（基础文本提取）
     */
    private String parsePdf(MultipartFile file) throws Exception {
        // 简单的PDF文本提取（实际项目中建议使用Apache PDFBox或iText）
        StringBuilder content = new StringBuilder();
        ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes());
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(bais, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 过滤PDF控制字符，提取可读文本
                String cleanLine = line.replaceAll("[^\\x20-\\x7E\\u4E00-\\u9FA5]", " ").trim();
                if (!cleanLine.isEmpty()) {
                    content.append(cleanLine).append("\n");
                }
            }
        }
        
        // 如果上述方法提取的内容太少，尝试使用简单标记
        if (content.length() < 100) {
            return extractTextFromPdfStructure(file.getBytes());
        }
        
        return content.toString();
    }
    
    /**
     * 从PDF结构中提取文本
     */
    private String extractTextFromPdfStructure(byte[] pdfBytes) {
        StringBuilder text = new StringBuilder();
        String content = new String(pdfBytes, StandardCharsets.ISO_8859_1);
        
        // 简单提取文本内容
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\(([^)]+)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String match = matcher.group(1);
            // 过滤控制字符
            if (match.matches(".*[\\u4E00-\\u9FA5]+.*") || match.matches(".*[a-zA-Z]+.*")) {
                text.append(match).append(" ");
            }
        }
        
        return text.length() > 0 ? text.toString() : "PDF文件内容提取失败，请确保PDF包含可提取的文本层";
    }
    
    /**
     * 解析DOCX文件（基于ZIP XML结构）
     */
    private String parseDocx(MultipartFile file) throws Exception {
        StringBuilder content = new StringBuilder();
        
        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    // 读取document.xml
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    
                    String xmlContent = baos.toString(StandardCharsets.UTF_8.name());
                    content.append(extractTextFromDocxXml(xmlContent));
                }
            }
        }
        
        return content.length() > 0 ? content.toString() : "DOCX文件内容提取失败";
    }
    
    /**
     * 从DOCX XML中提取文本
     */
    private String extractTextFromDocxXml(String xml) {
        StringBuilder text = new StringBuilder();
        
        // 使用简单的正则提取<w:t>标签中的文本
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<w:t[^>]*>([^<]*)</w:t>");
        java.util.regex.Matcher matcher = pattern.matcher(xml);
        
        while (matcher.find()) {
            String wText = matcher.group(1);
            if (!wText.trim().isEmpty()) {
                text.append(wText);
            }
        }
        
        return text.toString();
    }
    
    /**
     * 解析TXT文件
     */
    private String parseTxt(MultipartFile file) throws Exception {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }
    
    /**
     * 解析Markdown文件
     */
    private String parseMarkdown(MultipartFile file) throws Exception {
        // Markdown文件直接读取为文本
        return new String(file.getBytes(), StandardCharsets.UTF_8);
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
        return file.getSize() <= 10 * 1024 * 1024; // 10MB
    }
}
