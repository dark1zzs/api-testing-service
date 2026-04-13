package com.example.apitestingservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean success;
    private int statusCode;
    private String errorMessage;

    private LocalDateTime executedAt;

    @ManyToOne
    @JoinColumn(name = "test_id")
    private ApiTest apiTest;
}