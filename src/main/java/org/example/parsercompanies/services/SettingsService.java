package org.example.parsercompanies.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
public class SettingsService {

    @Value("${webdriver.chrome.driver}")
    private String chromeDriverPath;

    private String settingsFilePath = "src/main/resources/settingsConfig.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebDriver webDriver;
    @Getter
    private Map<String, Object> settings;

    public SettingsService() throws IOException {
        loadSettings();
    }

    private void loadSettings() throws IOException {
        File settingsFile = new File(settingsFilePath);
        if (!settingsFile.exists()) {
            throw new IOException("Settings file not found: " + settingsFilePath);
        }
        settings = objectMapper.readValue(settingsFile, Map.class);
    }

    private void saveSettings() throws IOException {
        File settingsFile = new File(settingsFilePath);
        objectMapper.writeValue(settingsFile, settings);
    }

    public void updateSettings(Map<String, Object> newSettings) throws IOException {
        settings.putAll(newSettings);
        saveSettings();
    }

    public WebDriver getWebDriver() throws IOException {
        if (webDriver == null) {
            reloadWebDriver();
        }
        return webDriver;
    }

    public void reloadWebDriver() throws IOException {
        loadSettings();

        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();

        String proxy = (String) settings.get("proxy");
        String proxyLogin = (String) settings.get("proxyLogin");
        String proxyPassword = (String) settings.get("proxyPassword");

        if (proxy != null && !proxy.isEmpty()) {
            String proxyConfig = proxy;
            if (proxyLogin != null && !proxyLogin.isEmpty()) {
                proxyConfig = proxyLogin + ":" + proxyPassword + "@" + proxy;
            }
            options.addArguments("--proxy-server=http://" + proxyConfig);
            System.out.println("Proxy enabled: " + proxy);
        } else {
            System.out.println("Proxy is not configured.");
        }

        if (webDriver != null) {
            webDriver.quit();
        }
        webDriver = new ChromeDriver(options);
    }

    @PreDestroy
    public void destroy() {
        if (webDriver != null) {
            webDriver.quit();
        }
    }
}
