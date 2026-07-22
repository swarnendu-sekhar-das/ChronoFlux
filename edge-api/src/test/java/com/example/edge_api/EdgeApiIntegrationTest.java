package com.example.edge_api;

import com.example.edge_api.domain.LiveEvent;
import com.example.edge_api.repository.LiveEventRepository;
import com.example.edge_api.service.LiveEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
public class EdgeApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @MockBean
    private LiveEventRepository repository;

    @Autowired
    private LiveEventService service;

    @Test
    public void testCacheStampedePrevention() {
        when(repository.findByStatusOrderByCreatedAtDesc("ACTIVE"))
                .thenReturn(List.of(new LiveEvent()));

        // First call hits the mock DB and caches the result
        service.getActiveEvents();

        // Second call should hit the cache, not the DB
        service.getActiveEvents();

        // Verify the DB was only called exactly once!
        verify(repository, times(1)).findByStatusOrderByCreatedAtDesc("ACTIVE");
    }
}
