package com.example.apitestingservice.repository;

import com.example.apitestingservice.entity.ApiTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiTestRepository extends JpaRepository<ApiTest, Long> {
    List<ApiTest> findByProjectIdOrderByRunOrderAscIdAsc(Long projectId);

    Optional<ApiTest> findByIdAndProjectId(Long id, Long projectId);
}