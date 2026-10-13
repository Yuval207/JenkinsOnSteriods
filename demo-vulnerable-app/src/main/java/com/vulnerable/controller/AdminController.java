package com.vulnerable.controller;

import org.springframework.web.bind.annotation.*;
import org.apache.commons.text.StringSubstitutor;

import java.util.Map;

/**
 * VULNERABLE: Exposes internal admin endpoints without authentication.
 * This violates architecture policy: admin service should not be externally accessible.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // VULNERABLE: No authentication on admin endpoints
    @GetMapping("/config")
    public Map<String, String> getConfig() {
        return Map.of(
                "db.host", "prod-db.internal.company.com",
                "db.port", "5432",
                "db.password", "production_password_do_not_share",
                "cache.host", "redis.internal.company.com"
        );
    }

    // VULNERABLE: Text4Shell via StringSubstitutor (CVE-2022-42889)
    @PostMapping("/template")
    public Map<String, String> processTemplate(@RequestBody Map<String, String> request) {
        String template = request.get("template");
        StringSubstitutor sub = new StringSubstitutor(
                Map.of("name", "SystemGuard", "version", "1.0")
        );
        String result = sub.replace(template);
        return Map.of("result", result);
    }

    // VULNERABLE: Exposes system information
    @GetMapping("/system")
    public Map<String, String> getSystemInfo() {
        return Map.of(
                "os", System.getProperty("os.name"),
                "java.version", System.getProperty("java.version"),
                "user.dir", System.getProperty("user.dir"),
                "user.name", System.getProperty("user.name")
        );
    }
}
