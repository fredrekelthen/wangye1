package com.edu.backend.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DatabaseService {

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/student_db}")
    private String mongoUri;

    private MongoClient getMongoClient() {
        try {
            return MongoClients.create(mongoUri);
        } catch (Exception e) {
            System.err.println("MongoDB连接失败: " + e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getDatabaseSchema() {
        Map<String, Object> schema = new HashMap<>();
        
        try {
            MongoClient client = getMongoClient();
            if (client == null) {
                schema.put("error", "无法连接到MongoDB数据库");
                return schema;
            }
            
            MongoDatabase database = client.getDatabase("student_db");
            
            schema.put("type", "MongoDB");
            schema.put("uri", mongoUri);
            schema.put("database", "student_db");
            schema.put("collections", getCollectionsInfo(database));
            
            client.close();
        } catch (Exception e) {
            schema.put("error", e.getMessage());
        }
        
        return schema;
    }

    public Map<String, Object> getCollections() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> collections = new ArrayList<>();
        
        try {
            MongoClient client = getMongoClient();
            if (client == null) {
                result.put("error", "无法连接到MongoDB数据库");
                return result;
            }
            
            MongoDatabase database = client.getDatabase("student_db");
            
            for (String collectionName : database.listCollectionNames()) {
                Map<String, Object> collection = new HashMap<>();
                collection.put("name", collectionName);
                
                MongoCollection<Document> coll = database.getCollection(collectionName);
                long count = coll.countDocuments();
                collection.put("count", count);
                
                Document sample = coll.find().first();
                if (sample != null) {
                    collection.put("fields", new ArrayList<>(sample.keySet()));
                }
                
                collections.add(collection);
            }
            
            result.put("data", collections);
            result.put("total", collections.size());
            
            client.close();
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    public Map<String, Object> executeQuery(String query) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (query.toLowerCase().contains("find") || query.contains("db.")) {
                result = executeMongoQuery(query);
            } else {
                result = analyzeDatabase(query);
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    private Map<String, Object> executeMongoQuery(String query) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> documents = new ArrayList<>();
        
        try {
            MongoClient client = getMongoClient();
            if (client == null) {
                result.put("error", "无法连接到MongoDB数据库");
                return result;
            }
            
            MongoDatabase database = client.getDatabase("student_db");
            
            String collectionName = extractCollectionName(query);
            if (collectionName != null) {
                MongoCollection<Document> collection = database.getCollection(collectionName);
                
                List<Document> docs = collection.find()
                    .limit(100)
                    .into(new ArrayList<>());
                
                for (Document doc : docs) {
                    documents.add(documentToMap(doc));
                }
                
                result.put("collection", collectionName);
                result.put("count", documents.size());
                result.put("data", documents);
            } else {
                result.put("error", "无法解析查询语句");
            }
            
            client.close();
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    private Map<String, Object> analyzeDatabase(String query) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            MongoClient client = getMongoClient();
            if (client == null) {
                analysis.put("error", "无法连接到MongoDB数据库");
                return analysis;
            }
            
            MongoDatabase database = client.getDatabase("student_db");
            
            if (query.contains("用户") || query.contains("user")) {
                analysis = analyzeCollection(database, "users");
            } else if (query.contains("课程") || query.contains("course")) {
                analysis = analyzeCollection(database, "courses");
            } else if (query.contains("资料") || query.contains("material")) {
                analysis = analyzeCollection(database, "materials");
            } else if (query.contains("进度") || query.contains("progress")) {
                analysis = analyzeCollection(database, "user_progress");
            } else if (query.contains("成绩") || query.contains("grade")) {
                analysis = analyzeCollection(database, "user_grades");
            } else {
                analysis.put("message", "请指定要分析的集合名称");
                analysis.put("available_collections", Arrays.asList(
                    "users", "courses", "course_structures", 
                    "materials", "user_progress", "user_grades"
                ));
            }
            
            client.close();
        } catch (Exception e) {
            analysis.put("error", e.getMessage());
        }
        
        return analysis;
    }

    private Map<String, Object> analyzeCollection(MongoDatabase database, String collectionName) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            analysis.put("collection", collectionName);
            analysis.put("total_documents", collection.countDocuments());
            
            Document sample = collection.find().first();
            if (sample != null) {
                analysis.put("fields", new ArrayList<>(sample.keySet()));
                analysis.put("sample_document", documentToMap(sample));
            }
            
            List<Map<String, Object>> indexes = new ArrayList<>();
            collection.listIndexes().forEach(idx -> indexes.add(documentToMap(idx)));
            analysis.put("indexes", indexes);
            
        } catch (Exception e) {
            analysis.put("error", e.getMessage());
        }
        
        return analysis;
    }

    private List<Map<String, Object>> getCollectionsInfo(MongoDatabase database) {
        List<Map<String, Object>> collections = new ArrayList<>();
        
        for (String name : database.listCollectionNames()) {
            Map<String, Object> info = new HashMap<>();
            info.put("name", name);
            
            try {
                MongoCollection<Document> collection = database.getCollection(name);
                Document sample = collection.find().first();
                
                if (sample != null) {
                    info.put("fields", new ArrayList<>(sample.keySet()));
                }
                
                info.put("count", collection.countDocuments());
            } catch (Exception e) {
                info.put("error", e.getMessage());
            }
            
            collections.add(info);
        }
        
        return collections;
    }

    private String extractCollectionName(String query) {
        int start = query.indexOf("db.") + 3;
        int end = query.indexOf(".");
        
        if (start > 2 && end > start) {
            return query.substring(start, end);
        }
        
        return null;
    }

    private Map<String, Object> documentToMap(Document doc) {
        Map<String, Object> map = new HashMap<>();
        
        for (String key : doc.keySet()) {
            Object value = doc.get(key);
            if (value instanceof Document) {
                map.put(key, documentToMap((Document) value));
            } else {
                map.put(key, value);
            }
        }
        
        return map;
    }
}
