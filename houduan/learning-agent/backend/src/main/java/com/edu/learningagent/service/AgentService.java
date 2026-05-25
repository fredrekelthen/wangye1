package com.edu.learningagent.service;

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

    @Value("${agent.frontend.path:../qianduan}")
    private String frontendPath;

    @Value("${agent.backend.path:../backend}")
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
        
        try {
            Path routerPath = Paths.get(frontendPath, "src/router/index.ts");
            if (Files.exists(routerPath)) {
                String content = Files.readString(routerPath);
                
                routeList.add(createRoute("/", "/", "home", "HomeView.vue", "首页"));
                routeList.add(createRoute("/course", "/course", "course", "CourseView.vue", "课程页面"));
                routeList.add(createRoute("/user-center", "/user-center", "user-center", "UserCenter.vue", "用户中心"));
                routeList.add(createRoute("/admin/material-manage", "/admin/material-manage", "admin-material-manage", "MaterialManage.vue", "资料管理"));
                routeList.add(createRoute("/admin/user-manage", "/admin/user-manage", "admin-user-manage", "UserManage.vue", "用户管理"));
            }
            
            routes.put("data", routeList);
            routes.put("total", routeList.size());
        } catch (Exception e) {
            routes.put("error", e.getMessage());
        }
        
        return routes;
    }

    public Map<String, Object> getComponents() {
        Map<String, Object> components = new HashMap<>();
        List<Map<String, String>> componentList = new ArrayList<>();
        
        try {
            addComponentsFromDirectory(Paths.get(frontendPath, "src/components"), "", componentList);
            addComponentsFromDirectory(Paths.get(frontendPath, "src/views"), "", componentList);
            addComponentsFromDirectory(Paths.get(frontendPath, "src/pages"), "", componentList);
            
            components.put("data", componentList);
            components.put("total", componentList.size());
        } catch (Exception e) {
            components.put("error", e.getMessage());
        }
        
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

    public void openComponentInEditor(String path) throws IOException {
        Path componentPath = findComponentFile(path);
        
        if (componentPath != null && Files.exists(componentPath)) {
            ProcessBuilder pb = new ProcessBuilder(
                "code", 
                componentPath.toAbsolutePath().toString()
            );
            pb.start();
        } else {
            throw new FileNotFoundException("组件文件未找到: " + path);
        }
    }

    public void navigateToPage(String page) {
        System.out.println("导航到页面: " + page);
    }

    public String getProjectContext() {
        StringBuilder context = new StringBuilder();
        
        context.append("=== 项目结构 ===\n");
        context.append("前端项目: ").append(frontendPath).append("\n");
        context.append("后端项目: ").append(backendPath).append("\n\n");
        
        context.append("=== 前端路由 ===\n");
        context.append("• / (home) - 首页\n");
        context.append("• /course (course) - 课程页面\n");
        context.append("• /user-center (user-center) - 用户中心\n");
        context.append("• /admin/material-manage - 资料管理\n");
        context.append("• /admin/user-manage - 用户管理\n\n");
        
        context.append("=== 数据库信息 ===\n");
        context.append("• 类型: MongoDB\n");
        context.append("• 连接: mongodb://localhost:27017/student_db\n");
        context.append("• 主要集合: users, courses, course_structures, materials, user_progress, user_grades\n\n");
        
        context.append("=== 技术栈 ===\n");
        context.append("• 前端: Vue 3 + TypeScript + Ant Design Vue\n");
        context.append("• 后端: Spring Boot 3.5.13 + Java 17\n");
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

    private void addComponentsFromDirectory(Path dir, String prefix, List<Map<String, String>> components) {
        if (!Files.exists(dir)) return;
        
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".vue") || p.toString().endsWith(".ts"))
                 .forEach(p -> {
                     Map<String, String> component = new HashMap<>();
                     String relativePath = prefix + p.getFileName().toString();
                     component.put("name", p.getFileName().toString().replace(".vue", "").replace(".ts", ""));
                     component.put("path", p.toString().replace(frontendPath + "/src/", ""));
                     components.add(component);
                 });
        } catch (IOException e) {
            System.err.println("Error reading directory: " + dir);
        }
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
