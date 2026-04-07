package com.example.apitestingservice.repository;

import com.example.apitestingservice.entity.ApiTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiTestRepository extends JpaRepository<ApiTest, Long> {
    List<ApiTest> findByProjectId(Long projectId);
}