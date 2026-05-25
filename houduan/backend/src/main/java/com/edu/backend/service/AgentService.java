package com.edu.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Service
public class AgentService {

    @Value("${agent.frontend.path:../../qianduan}")
    private String frontendPath;

    @Value("${agent.backend.path:.}")
    private String backendPath;

    public Map<String, Object> getProjectStructure() {
        Map<String, Object> structure = new HashMap<>();
        
        try {
            Map<String, Object> frontend = new HashMap<>();
            frontend.put("path", frontendPath);
            frontend.put("files", listFiles(frontendPath, 3));
            structure.put("frontend", frontend);
            
            Map<String, Object> backend = new HashMap<>();
            backend.put("path", backendPath);
            backend.put("files", listFiles(backendPath, 3));
            structure.put("backend", backend);
            
        } catch (Exception e) {
            structure.put("error", e.getMessage());
        }
        
        return structure;
    }

    public Map<String, Object> getRoutes() {
        Map<String, Object> routes = new HashMap<>();
        List<Map<String, String>> routeList = new ArrayList<>();
        
        routeList.add(createRoute("/", "/", "home", "HomeView.vue", "首页"));
        routeList.add(createRoute("/course", "/course", "course", "CourseView.vue", "课程页面"));
        routeList.add(createRoute("/user-center", "/user-center", "user-center", "UserCenter.vue", "用户中心"));
        routeList.add(createRoute("/admin/material-manage", "/admin/material-manage", "admin-material-manage", "MaterialManage.vue", "资料管理"));
        routeList.add(createRoute("/admin/user-manage", "/admin/user-manage", "admin-user-manage", "UserManage.vue", "用户管理"));
        
        routes.put("data", routeList);
        routes.put("total", routeList.size());
        
        return routes;
    }

    public Map<String, Object> getComponents() {
        Map<String, Object> components = new HashMap<>();
        List<Map<String, String>> componentList = new ArrayList<>();
        
        componentList.add(createComponent("CourseContent", "components/course/CourseContent.vue"));
        componentList.add(createComponent("CourseDiscuss", "components/course/CourseDiscuss.vue"));
        componentList.add(createComponent("CourseGrade", "components/course/CourseGrade.vue"));
        componentList.add(createComponent("CourseGroup", "components/course/CourseGroup.vue"));
        componentList.add(createComponent("CourseHeader", "components/course/CourseHeader.vue"));
        componentList.add(createComponent("CourseNotice", "components/course/CourseNotice.vue"));
        componentList.add(createComponent("CourseTabs", "components/course/CourseTabs.vue"));
        componentList.add(createComponent("CourseTree", "components/course/CourseTree.vue"));
        componentList.add(createComponent("KnowledgeGraph", "components/course/KnowledgeGraph.vue"));
        componentList.add(createComponent("AuthModal", "components/AuthModal.vue"));
        componentList.add(createComponent("GlobalHeader", "components/GlobalHeader.vue"));
        componentList.add(createComponent("MaterialModal", "components/MaterialModal.vue"));
        componentList.add(createComponent("HomeView", "views/HomeView.vue"));
        componentList.add(createComponent("CourseView", "views/CourseView.vue"));
        componentList.add(createComponent("UserCenter", "views/UserCenter.vue"));
        componentList.add(createComponent("MaterialManage", "pages/admin/MaterialManage.vue"));
        componentList.add(createComponent("UserManage", "pages/admin/UserManage.vue"));
        
        components.put("data", componentList);
        components.put("total", componentList.size());
        
        return components;
    }

    public Map<String, Object> analyzeComponent(String path) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            Path componentPath = findComponentFile(path);
            
            if (componentPath != null && Files.exists(componentPath)) {
                String content = Files.readString(componentPath);
                
                analysis.put("path", componentPath.toString());
                analysis.put("name", componentPath.getFileName().toString());
                analysis.put("type", getFileType(componentPath));
                analysis.put("lines", content.split("\n").length);
                analysis.put("code", content);
                analysis.put("description", analyzeCode(content));
                analysis.put("dependencies", extractDependencies(content));
            } else {
                analysis.put("error", "组件文件未找到: " + path);
            }
        } catch (Exception e) {
            analysis.put("error", e.getMessage());
        }
        
        return analysis;
    }

    public void navigateToPage(String page) {
        System.out.println("导航到页面: " + page);
    }

    public String getProjectContext() {
        StringBuilder context = new StringBuilder();
        
        context.append("=== 项目导航 ===\n");
        context.append("• 首页 - 展示课程列表和学习进度\n");
        context.append("• 课程页面 - 学习课程内容\n");
        context.append("• 用户中心 - 个人信息和学习记录\n");
        context.append("• 资料管理 - 管理课程资料（管理员）\n");
        context.append("• 用户管理 - 管理用户账号（管理员）\n\n");
        
        context.append("=== 功能模块 ===\n");
        context.append("• 课程学习: 支持课程内容浏览、章节导航、知识点学习\n");
        context.append("• 用户管理: 用户注册、登录、个人信息管理\n");
        context.append("• 资料管理: 课程资料上传、下载、分类管理\n");
        context.append("• 学习进度: 记录学习时长、完成状态\n\n");
        
        context.append("=== 技术栈 ===\n");
        context.append("• 前端: Vue 3 + TypeScript + Ant Design Vue\n");
        context.append("• 后端: Spring Boot + Java 17\n");
        context.append("• 数据库: MongoDB\n");
        
        return context.toString();
    }

    private List<String> listFiles(String dirPath, int maxDepth) throws IOException {
        List<String> files = new ArrayList<>();
        Path path = Paths.get(dirPath);
        
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path, maxDepth)) {
                walk.filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(f -> !f.contains(".git"))
                    .forEach(files::add);
            }
        }
        
        return files;
    }

    private Map<String, String> createRoute(String path, String fullPath, String name, String component, String description) {
        Map<String, String> route = new HashMap<>();
        route.put("path", path);
        route.put("name", name);
        route.put("component", component);
        route.put("description", description);
        return route;
    }

    private Map<String, String> createComponent(String name, String path) {
        Map<String, String> component = new HashMap<>();
        component.put("name", name);
        component.put("path", path);
        return component;
    }

    private Path findComponentFile(String path) {
        String[] searchPaths = {
            frontendPath + "/src/" + path,
            frontendPath + "/src/" + path + ".vue",
            frontendPath + "/src/views/" + path,
            frontendPath + "/src/views/" + path + ".vue",
            frontendPath + "/src/components/" + path,
            frontendPath + "/src/components/" + path + ".vue",
            frontendPath + "/src/pages/" + path,
            frontendPath + "/src/pages/" + path + ".vue",
        };
        
        for (String searchPath : searchPaths) {
            Path p = Paths.get(searchPath);
            if (Files.exists(p)) {
                return p;
            }
        }
        
        return null;
    }

    private String getFileType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".vue")) return "Vue Component";
        if (name.endsWith(".ts")) return "TypeScript";
        if (name.endsWith(".java")) return "Java";
        return "Unknown";
    }

    private String analyzeCode(String content) {
        StringBuilder analysis = new StringBuilder();
        
        if (content.contains("<template>")) {
            analysis.append("这是一个Vue组件，");
        }
        if (content.contains("<script")) {
            analysis.append("包含JavaScript/TypeScript逻辑，");
        }
        if (content.contains("<style")) {
            analysis.append("包含样式定义。");
        }
        
        return analysis.toString();
    }

    private List<String> extractDependencies(String content) {
        List<String> deps = new ArrayList<>();
        
        if (content.contains("import ")) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.trim().startsWith("import ") && line.contains("from")) {
                    String dep = line.substring(line.indexOf("from") + 5)
                                    .replace("'", "").replace("\"", "").trim();
                    if (!dep.startsWith(".") && !dep.startsWith("@")) {
                        deps.add(dep);
                    }
                }
            }
        }
        
        return deps;
    }
}
