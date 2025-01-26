package org.example.parsercompanies.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.parsercompanies.model.InfoJson;
import org.example.parsercompanies.model.SettingsConfig;
import org.example.parsercompanies.model.db.Category;
import org.example.parsercompanies.repos.CategoryRepository;
import org.example.parsercompanies.services.SettingsService;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    @Autowired
    private CategoryRepository categoryRepository;
    private WebDriver webDriver;
    private final AtomicBoolean isParsing = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void loadParsingInfo() throws IOException {
        File infoFile = new File(INFO_FILE);
        if (infoFile.exists()) {
            InfoJson info = objectMapper.readValue(infoFile, InfoJson.class);
            this.setCurrentPage(info.getCurrentPage());
            this.setCompaniesParsed(info.isCompaniesParsed());
        } else {
            System.err.println("Info file not found: " + INFO_FILE);
        }

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

    public void startParsing() {
        if (isParsing.get()) {
            System.out.println("Parsing is already running.");
            return;
        }

        isParsing.set(true);
        executorService.submit(() -> {
            try {
                loadParsingInfo();
                webDriver = new ChromeDriver(settingsService.getOptions());
                webDriver.manage().window().maximize();
                webDriver.get("https://checko.ru/search/advanced");
                applyFilters();
                extractAndSaveCompanyLinks();
            } catch (Exception e) {
                System.err.println("Error during parsing: " + e.getMessage());
            } finally {
//                stopParsing();
            }
        });
    }

    private void applyFilters() throws IOException, InterruptedException {
        if (!isParsing.get()) return;
        applyCategories();
        if (!isParsing.get()) return;
        applyCities();
        if(!isParsing.get()) return;
        checkAndToggleCheckbox(webDriver,"input-9");

    }

    private void applyCategories() throws IOException, InterruptedException {
        webDriver.findElement(By.id("input-11")).click();
        if (!isParsing.get()) return;

        List<Category> activeCategories = categoryRepository.findAllByActive(true);
        Set<String> categoriesNames = activeCategories.stream()
                .map(category -> category.getName().substring(category.getName().indexOf(" ") + 1))
                .collect(Collectors.toSet());

        for (String categoryName : categoriesNames) {
            if (!isParsing.get()) break;

            WebElement searchInput = webDriver.findElement(By.id("activity_search"));
            searchInput.sendKeys(Keys.CONTROL + "a"); // Выделяем весь текст в поле
            searchInput.sendKeys(Keys.DELETE); // Удаляем выделенный текст
            searchInput.clear();
            searchInput.sendKeys(categoryName);

            List<WebElement> results = webDriver.findElements(By.cssSelector(".v-treeview-node"));
            for (WebElement result : results) {
                WebElement label = result.findElement(By.cssSelector(".v-treeview-node__label"));
                String foundCategory = label.getText();

                if (foundCategory.contains(categoryName)) {
                    WebElement checkbox = result.findElement(By.cssSelector(".v-treeview-node__checkbox"));
                    String checkboxClass = checkbox.getAttribute("class");
                    if (!checkboxClass.contains("mdi-checkbox-marked")) {
                        checkbox.click();
                    }
                    break;
                }
            }
        }
        webDriver.findElement(By.xpath("/html/body/main/div[2]/div/div[4]/div/div/div[2]/div/div/div[3]/div[2]/button")).click();
    }

    private void applyCities() {
        webDriver.findElement(By.id("input-13")).click();
        for (String cityName : cities) {
            if (!isParsing.get()) break;
            try {
                WebElement searchInput = webDriver.findElement(By.id("location_search"));
                searchInput.clear();
                searchInput.sendKeys(Keys.CONTROL + "a"); // Выделяем весь текст в поле
                searchInput.sendKeys(Keys.DELETE); // Удаляем выделенный текст
                Thread.sleep((long)(parsingDelay * 1000));
                searchInput.sendKeys(cityName);

                List<WebElement> results = webDriver.findElements(By.cssSelector(".v-treeview-node"));
                for (WebElement result : results) {
                    WebElement label = result.findElement(By.cssSelector(".v-treeview-node__label"));
                    String foundCity = label.getText().trim();

                    if (foundCity.contains(cityName)) {
                        WebElement checkbox = result.findElement(By.cssSelector(".v-treeview-node__checkbox"));
                        String checkboxClass = checkbox.getAttribute("class");
                        if (!checkboxClass.contains("mdi-checkbox-marked")) {
                            checkbox.click();
                        }
                        break;
                    }
                }
                Thread.sleep(parsingDelay.longValue()  * 1000);
            } catch (Exception e) {
                System.err.println("Error applying city filter: " + cityName);
            }
        }
        webDriver.findElement(By.xpath("/html/body/main/div[2]/div/div[5]/div/div/div[2]/div/div/div[3]/div[2]/button")).click();
    }

    public void extractAndSaveCompanyLinks() {
        if (!isParsing.get()) return;

        int page = currentPage != null ? currentPage.intValue() : 1;
        int pagesToParse = pagesDeep != null ? pagesDeep.intValue() : Integer.MAX_VALUE;
        companiesParsed = false;

        while (isParsing.get() && page <= pagesToParse) {
            try {
                webDriver.get("https://checko.ru/search/advanced?page=" + page);

                List<WebElement> notFoundElements = webDriver.findElements(By.xpath("/html/body/main/div[2]/div/div[2]/div/p"));
                if (!notFoundElements.isEmpty() && notFoundElements.get(0).getText().contains("Не найдено ни одного юридического лица")) {
                    System.out.println("No more companies found. Stopping pagination.");
                    break;
                }

                List<WebElement> rows = webDriver.findElements(By.cssSelector("table.table-lg tbody tr"));
                if (rows.isEmpty()) {
                    System.out.println("No rows found on page " + page);
                    break;
                }

                List<String> companyLinks = new ArrayList<>();

                for (WebElement row : rows) {
                    try {
                        WebElement linkElement = row.findElement(By.cssSelector("a.link"));
                        String link = linkElement.getAttribute("href");
                        companyLinks.add(link);
                        System.out.println("Extracted link: " + link);

                        // Сохраняем в базу данных
                        // Здесь добавить сохранение в базу через репозиторий или другой сервис
                    } catch (Exception e) {
                        System.err.println("Error extracting link from row: " + e.getMessage());
                    }
                }

                System.out.println("Page " + page + " processed. Total links extracted: " + companyLinks.size());
                page++;
                saveProgress(page, false);

                Thread.sleep(parsingDelay.longValue() * 1000);

            } catch (Exception e) {
                System.err.println("Error processing page " + page + ": " + e.getMessage());
                break;
            }
        }

        companiesParsed = true;
        saveProgress(page, true);
    }

    private void saveProgress(int currentPage, boolean isCompleted) {
        try {
            InfoJson infoJson = new InfoJson();
            infoJson.setCurrentPage((long) currentPage);
            infoJson.setCompaniesParsed(isCompleted);
            objectMapper.writeValue(new File(INFO_FILE), infoJson);
        } catch (IOException e) {
            System.err.println("Error saving progress: " + e.getMessage());
        }
    }

    public void stopParsing() {
        isParsing.set(false);
        if (webDriver != null) {
            webDriver.quit();
        }
    }
    private void checkAndToggleCheckbox(WebDriver driver, String checkboxId) {
        try {
            WebElement checkbox = driver.findElement(By.id(checkboxId));
            String isChecked = checkbox.getAttribute("aria-checked");
            if ("false".equals(isChecked)) {
                try {
                    checkbox.click();
                    System.out.println("Checkbox with id " + checkboxId + " has been toggled.");
                } catch (Exception e) {
                    System.out.println("Click intercepted, trying JavaScript...");
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", checkbox);
                    System.out.println("Checkbox with id " + checkboxId + " has been toggled using JavaScript.");
                }
            } else {
                System.out.println("Checkbox with id " + checkboxId + " is already checked.");
            }
        } catch (Exception e) {
            System.err.println("Error interacting with checkbox: " + e.getMessage());
        }
    }
}
