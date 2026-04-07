package com.example.apitestingservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ApiTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String testKey;
    private String method;
    private String endpoint;
    private Integer expectedStatus;

    @ManyToOne  //у одного проекта может быть много тестов
    @JoinColumn(name = "project_id")
    private Project project;

    public ApiTest() {
    }
}