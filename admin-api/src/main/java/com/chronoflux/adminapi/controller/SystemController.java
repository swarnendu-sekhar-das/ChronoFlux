package com.chronoflux.adminapi.controller;

import com.chronoflux.adminapi.service.SystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chronoflux.adminapi.dto.SystemStatusDTO;

@RestController
@RequestMapping("/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/status")
    public ResponseEntity<SystemStatusDTO> getSystemStatus() {
        return ResponseEntity.ok(systemService.getSystemStatus());
    }
}
