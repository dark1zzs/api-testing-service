package com.example.apitestingservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ApiTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;
    private String description;
    private String testKey;

    @NotBlank
    @Pattern(regexp = "GET|POST|PUT|DELETE", message = "Method must be one of: GET, POST, PUT, DELETE")
    private String method;

    @NotBlank
    private String endpoint;

    @Lob
    private String requestBody;

    @Lob
    private String expectedResponseBody;

    private String expectedJsonPath;
    private String expectedJsonValue;

    private String expectedHeaderName;
    private String expectedHeaderValue;

    private Long maxResponseTimeMs;

    @NotNull
    private Integer expectedStatus;

    @ManyToOne  //у одного проекта может быть много тестов
    @JoinColumn(name = "project_id")
    private Project project;

    public ApiTest() {
    }
}
