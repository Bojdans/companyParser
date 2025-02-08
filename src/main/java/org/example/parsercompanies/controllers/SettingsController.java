package org.example.parsercompanies.controllers;

import org.example.parsercompanies.services.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    @Autowired
    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    
    @PostMapping
    public ResponseEntity<String> updateSettings(@RequestBody Map<String, Object> newSettings) {
        try {
            settingsService.updateSettings(newSettings);
            settingsService.reloadWebDriver();
            return ResponseEntity.ok("Settings updated successfully.");
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Failed to update settings: " + e.getMessage());
        }
    }
}
