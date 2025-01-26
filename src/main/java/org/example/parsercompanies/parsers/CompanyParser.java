package org.example.parsercompanies.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.parsercompanies.model.InfoJson;
import org.example.parsercompanies.model.SettingsConfig;
import org.example.parsercompanies.services.SettingsService;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
@Data
@NoArgsConstructor
public class CompanyParser {
    @Autowired
    private SettingsService settingsService;

    private Long pagesDeep;
    private Long currentPage;
    private boolean companiesParsed;
    private Double parsingDelay;
    private List<String> cities;
    private List<String> regions;
    private boolean onlyMainOKVED;
    private boolean onlyInOperation;
    private boolean partOfGovernmentProcurement;

    private static final String INFO_FILE = "src/main/resources/info.json"; // Укажите путь к info.json
    private static final String SETTINGS_FILE = "src/main/resources/settingsConfig.json"; // Укажите путь к settingsConfig.json
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void loadParsingInfo() throws IOException {
        // Чтение info.json
        File infoFile = new File(INFO_FILE);
        if (infoFile.exists()) {
            InfoJson info = objectMapper.readValue(infoFile, InfoJson.class);
            this.setCurrentPage(info.getCurrentPage());
            this.setCompaniesParsed(info.isCompaniesParsed());
        } else {
            System.err.println("Info file not found: " + INFO_FILE);
        }

        // Чтение settingsConfig.json
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            SettingsConfig settings = objectMapper.readValue(settingsFile, SettingsConfig.class);
            this.setPagesDeep(settings.getPagesDeep());
            this.setParsingDelay(settings.getParsingDelay());
            this.setCities(settings.getCities());
            this.setRegions(settings.getRegions());
            this.setOnlyMainOKVED(settings.isOnlyMainOKVED());
            this.setOnlyInOperation(settings.isOnlyInOperation());
            this.setPartOfGovernmentProcurement(settings.isPartOfGovernmentProcurement());
        } else {
            System.err.println("Settings file not found: " + SETTINGS_FILE);
        }
    }

    public void startParsing() throws IOException {
//        WebDriver webDriver = new ChromeDriver(settingsService.getOptions().addArguments("--headless"));
        WebDriver webDriver = new ChromeDriver(settingsService.getOptions());
        webDriver.manage().window().maximize();
        webDriver.get("https://checko.ru/search/advanced");
    }
}
//переключение между страницами отфильтрованных данных по параметру ссылки page=
//парсинг сначала всех компаний потом уже данных этих компаний
//фильтрация по городам хранится в настройках, рубрики в базе