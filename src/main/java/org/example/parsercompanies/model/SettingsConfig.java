package org.example.parsercompanies.model;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.Map;
@Data
public class SettingsConfig {
    private boolean onlyInOperation;
    private boolean partOfGovernmentProcurement;
    private int pagesDeep;
    private double parsingDelay;
    private boolean autoExcelOpen;
    private boolean onlyMainOKVED;
    private String proxy;
    private String proxyLogin;
    private String proxyPassword;
    private Map<String, Object> cities;
    private Map<String, Object> rubrics;

    // Методы для сохранения и загрузки данных
    public static SettingsConfig loadFromFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(filePath), SettingsConfig.class);
    }

    public void saveToFile(String filePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), this);
    }
}
