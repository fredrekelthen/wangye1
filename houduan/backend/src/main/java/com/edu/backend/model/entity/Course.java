package com.edu.backend.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "courses")
public class Course {
    @Id
    private String id;
    private String courseName;
    private String teacherId;
    // 省略 getter/setter (如果你用了 Lombok,只需在类头加 @Data 注解)
}
