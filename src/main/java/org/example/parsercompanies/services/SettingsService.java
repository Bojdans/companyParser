package org.example.parsercompanies.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.Getter;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class SettingsService {

    @Value("${webdriver.chrome.driver}")
    @Getter
    private String chromeDriverPath;
    private String settingsFilePath = Paths.get(System.getProperty("user.dir"), "cfg", "settingsConfig.json").toString();
    @Getter
    private boolean configured = false;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Getter
    private Map<String, Object> settings;
    @Getter
    private ChromeOptions options;
    @PostConstruct
    public void openBrowser() {
        String url = "http://localhost:8081/page";
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", url).start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при открытии браузера: " + e.getMessage());
        }
    }

    public SettingsService() throws IOException {
        loadSettings();
    }

    public void loadSettings() throws IOException {
        File settingsFile = new File(settingsFilePath);
        if (!settingsFile.exists()) {
            throw new IOException("Settings file not found: " + settingsFilePath);
        }
        settings = objectMapper.readValue(settingsFile, Map.class);
        options = new ChromeOptions();
    }

    private void saveSettings() throws IOException {
        File settingsFile = new File(settingsFilePath);
        objectMapper.writeValue(settingsFile, settings);
    }

    public void updateSettings(Map<String, Object> newSettings) throws IOException {
        settings.putAll(newSettings);
        saveSettings();
    }

    public void reloadWebDriver() throws IOException {
        configured = false;
        loadSettings();

        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        options.addArguments("--window-size=1920,1080"); // Фиксированный размер окна
        options.addArguments("--disable-blink-features=AutomationControlled"); // Обход детекта автоматизации
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36..."); // Маскировка под обычный браузер
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments("--remote-debugging-port=9222");

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
        configured = true;
    }
}
