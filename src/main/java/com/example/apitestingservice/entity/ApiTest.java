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

    /**
     * Optional JSON object of request header name → value, e.g. {"Authorization":"{{token}}"}.
     * Values may contain {@code {{var}}} placeholders filled during a project batch run.
     */
    @Lob
    private String requestHeadersJson;

    /**
     * When set (with {@link #captureVariableName}), a successful response body is read at this JSONPath
     * and stored into the batch run context under {@link #captureVariableName}.
     */
    private String captureJsonPath;

    private String captureVariableName;

    /**
     * Lower values run first within a project when using "run all tests".
     */
    @Column(nullable = false)
    private int runOrder;

    @Lob
    private String expectedResponseBody;

    private String expectedJsonPath;
    private String expectedJsonValue;

    private String expectedHeaderName;
    private String expectedHeaderValue;

    private Long maxResponseTimeMs;

    @NotNull
    private Integer expectedStatus;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    public ApiTest() {
        this.runOrder = 0;
    }
}
