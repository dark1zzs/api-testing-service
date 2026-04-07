package com.example.apitestingservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
public class Project {

    //Это сущность JPA, который будет сохраняться в базу данных как таблица project

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //идентификатор

    private String name; //название проекта
    @NotBlank
    private String baseUrl; //базовый URL тестируемого API
    private String description; //описание

    public Project() {
    }

    public Project(Long id, String name, String baseUrl, String description) {
        this.id = id;
        this.name = name;
        this.baseUrl = baseUrl;
        this.description = description;
    }

}