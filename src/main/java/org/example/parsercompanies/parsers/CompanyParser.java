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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
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
    private boolean linksOfCompaniesParsed;
    private Double parsingDelay;
    private List<String> cities;
    private List<String> regions;
    private boolean onlyMainOKVED;
    private boolean onlyInOperation;
    private boolean partOfGovernmentProcurement;
    private boolean rememberParsingPosition;

    private static final String INFO_FILE = "src/main/resources/info.json";
    private static final String SETTINGS_FILE = "src/main/resources/settingsConfig.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    private WebDriver webDriver;
    private final AtomicBoolean isParsing = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Установите здесь желаемое время ожидания в секундах
    private static final int WAIT_TIMEOUT_SECONDS = 60;

    public void loadParsingInfo() throws IOException {
        File infoFile = new File(INFO_FILE);
        if (infoFile.exists()) {
            InfoJson info = objectMapper.readValue(infoFile, InfoJson.class);
            this.setCurrentPage(info.getCurrentPage());
            this.setCompaniesParsed(info.isCompaniesParsed());
            this.setLinksOfCompaniesParsed(info.isLinksParsed());
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
            this.setRememberParsingPosition(settings.isRememberParsingPosition());
        } else {
            System.err.println("Settings file not found: " + SETTINGS_FILE);
        }
    }

    public void startParsing() {
        if (isParsing.get()) {
            System.out.println("Parsing is already running.");
            return;
        }
        companiesParsed = false;
        isParsing.set(true);
        executorService.submit(() -> {
            try {
                loadParsingInfo();
                webDriver = new ChromeDriver(settingsService.getOptions());
                webDriver.manage().window().maximize();

                // Переходим на страницу и сразу проверяем 500-ю ошибку
                webDriver.get("https://checko.ru/search/advanced");
                checkForServerErrorAndRefreshIfNeeded();

                applyFilters();
                extractAndSaveCompanyLinks();
            } catch (Exception e) {
                System.err.println("Error during parsing: " + e.getMessage());
            } finally {
                companiesParsed = true;
                stopParsing();
            }
        });
    }

    private void applyFilters() throws IOException, InterruptedException {
        if (!isParsing.get()) return;

        checkAndToggleCheckbox();
        checkForServerErrorAndRefreshIfNeeded(); // проверяем 500-ю ошибку

        if (!isParsing.get()) return;
        applyCities();
        checkForServerErrorAndRefreshIfNeeded();

        if (!isParsing.get()) return;
        applyCategories();
        checkForServerErrorAndRefreshIfNeeded();

        if (!isParsing.get()) return;
        if (partOfGovernmentProcurement) {
            checkAndTogglePartOfGovernmentProcurement();
            checkForServerErrorAndRefreshIfNeeded();
        }
    }

    private void applyCategories() throws IOException, InterruptedException {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
        Thread.sleep(2000);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("input-11")));
        WebElement categoriesButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("input-11"))
        );
        categoriesButton.click();
        checkForServerErrorAndRefreshIfNeeded();

        if (!isParsing.get()) return;

        List<Category> activeCategories = categoryRepository.findAllByActive(true);
        Set<String> categoriesNames = activeCategories.stream()
                .map(category -> category.getName().substring(category.getName().indexOf(" ") + 1))
                .collect(Collectors.toSet());

        for (String categoryName : categoriesNames) {
            if (!isParsing.get()) break;

            WebElement searchInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("activity_search"))
            );

            searchInput.sendKeys(Keys.CONTROL + "a");
            searchInput.sendKeys(Keys.DELETE);
            searchInput.sendKeys(categoryName);

            // Ищем результаты
            List<WebElement> results = webDriver.findElements(By.cssSelector(".v-treeview-node"));
            for (WebElement result : results) {
                try {
                    WebElement label = result.findElement(By.cssSelector(".v-treeview-node__label"));
                    String foundCategory = label.getText();

                    if (foundCategory.contains(categoryName)) {
                        WebElement checkbox = result.findElement(By.cssSelector(".v-treeview-node__checkbox"));
                        String checkboxClass = checkbox.getAttribute("class");
                        if (!checkboxClass.contains("mdi-checkbox-marked")) {
                            checkbox.click();
                            checkForServerErrorAndRefreshIfNeeded();
                        }
                        break;
                    }
                } catch (NoSuchElementException ignored) {
                }
            }
        }

        // Кнопка "Выбрать"
        WebElement selectButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("/html/body/main/div[2]/div/div[4]/div/div/div[2]/div/div/div[3]/div[2]/button")
                )
        );
        selectButton.click();
        checkForServerErrorAndRefreshIfNeeded();
    }

    private void applyCities() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
        Thread.sleep(2000);

        wait.until(driver -> {
            try {
                WebElement element = wait.until(
                        ExpectedConditions.presenceOfElementLocated(By.id("input-13"))
                );
                if (element.isDisplayed() && element.isEnabled()) {
                    element.click();
                    return true;
                }
                return false;
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        checkForServerErrorAndRefreshIfNeeded();

        for (String cityName : cities) {
            if (!isParsing.get()) break;
            try {
                WebElement searchInput = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(By.id("location_search"))
                );

                searchInput.sendKeys(Keys.CONTROL + "a");
                searchInput.sendKeys(Keys.DELETE);

                searchInput.sendKeys(cityName);
                checkForServerErrorAndRefreshIfNeeded();

                // Ждём появления списка
                List<WebElement> results = webDriver.findElements(By.cssSelector(".v-treeview-node"));
                for (WebElement result : results) {
                    WebElement label = result.findElement(By.cssSelector(".v-treeview-node__label"));
                    String foundCity = label.getText().trim();

                    if (foundCity.contains(cityName)) {
                        WebElement checkbox = result.findElement(By.cssSelector(".v-treeview-node__checkbox"));
                        String checkboxClass = checkbox.getAttribute("class");
                        if (!checkboxClass.contains("mdi-checkbox-marked")) {
                            checkbox.click();
                            checkForServerErrorAndRefreshIfNeeded();
                        }
                        break;
                    }
                }
                Thread.sleep((long) (parsingDelay * 1000));

            } catch (Exception e) {
                System.err.println("Error applying city filter: " + cityName + " => " + e.getMessage());
            }
        }

        WebElement selectButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("/html/body/main/div[2]/div/div[5]/div/div/div[2]/div/div/div[3]/div[2]/button")
                )
        );
        selectButton.click();
        checkForServerErrorAndRefreshIfNeeded();
    }

    public void extractAndSaveCompanyLinks() {
        if (!isParsing.get()) return;
        System.out.println("Переходим к парсингу...");
        System.out.println(currentPage);
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));

        while (isParsing.get() && currentPage <= pagesDeep) {
            try {
                webDriver.get("https://checko.ru/search/advanced?page=" + currentPage);
                checkForServerErrorAndRefreshIfNeeded();

                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("main")));
                checkForServerErrorAndRefreshIfNeeded();

                List<WebElement> notFoundElements = webDriver.findElements(
                        By.xpath("/html/body/main/div[2]/div/div[2]/div/p")
                );
                if (!notFoundElements.isEmpty()
                        && notFoundElements.get(0).getText().contains("Не найдено ни одного юридического лица")) {
                    System.out.println("No more companies found. Stopping pagination.");
                    break;
                }

                // Находим таблицу
                List<WebElement> rows = webDriver
                        .findElement(By.className("select-section"))
                        .findElements(By.cssSelector("table.table-lg tbody tr"));

                if (rows.isEmpty()) {
                    System.out.println("No rows found on currentPage " + currentPage);
                    break;
                }

                List<String> companyLinks = new ArrayList<>();
                for (WebElement row : rows) {
                    try {
                        WebElement linkElement = row.findElement(By.cssSelector("a.link"));
                        String link = linkElement.getAttribute("href");
                        companyLinks.add(link);
                        System.out.println("Extracted link: " + link);
                        // Сохраняем в БД, если нужно
                    } catch (Exception e) {
                        System.err.println("Error extracting link from row: " + e.getMessage());
                    }

                    checkForServerErrorAndRefreshIfNeeded();
                }

                System.out.println("Page " + currentPage + " processed. Total links extracted: " + companyLinks.size());
                currentPage++;
                if (rememberParsingPosition){
                    saveProgress(currentPage, false, false);
                }
                Thread.sleep((long) (parsingDelay * 1000));

            } catch (Exception e) {
                System.err.println("Error processing currentPage " + currentPage + ": " + e.getMessage());
                break;
            }
        }
        setLinksOfCompaniesParsed(true);

        if (rememberParsingPosition){
            saveProgress(currentPage, true,true);
        }
    }

    private void saveProgress(Long currentPage, boolean isCompleted, boolean isLinksParsed) {
        try {
            InfoJson infoJson = new InfoJson();
            infoJson.setCurrentPage(currentPage);
            infoJson.setCompaniesParsed(isCompleted);
            infoJson.setLinksParsed(isLinksParsed);
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

    private void checkAndToggleCheckbox() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
        Thread.sleep(2000);
        WebElement checkbox = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("input-9"))
        );
        checkForServerErrorAndRefreshIfNeeded();

        String isChecked = checkbox.getAttribute("aria-checked");
        if ("false".equals(isChecked)) {
            WebElement label = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("label[for=\"input-9\"]"))
            );
            label.click();
            checkForServerErrorAndRefreshIfNeeded();
        }
    }

    private void checkAndTogglePartOfGovernmentProcurement() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
        Thread.sleep(2000);

        WebElement governmentProcurement = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("/html/body/main/div[2]/div/div[1]/div/div/div/div[6]/div[7]/strong/button")
                )
        );
        governmentProcurement.click();
        checkForServerErrorAndRefreshIfNeeded();

        WebElement checkbox = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("input-55"))
        );
        checkForServerErrorAndRefreshIfNeeded();

        String isChecked = checkbox.getAttribute("aria-checked");
        if ("false".equals(isChecked)) {
            WebElement label = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("label[for=\"input-55\"]"))
            );
            label.click();
            checkForServerErrorAndRefreshIfNeeded();
        }
    }

    /**
     * Проверяет, не появился ли на странице блок с текстом "500 Internal Server Error".
     * Если появился — несколько раз пытается перезагрузить страницу, пока ошибка не пропадёт.
     */
    private void checkForServerErrorAndRefreshIfNeeded() throws InterruptedException {
        final int MAX_REFRESH_ATTEMPTS = 5;
        int attempt = 0;

        while (attempt < MAX_REFRESH_ATTEMPTS && isParsing.get()) {
            // Ищем элемент <pre>, в тексте которого есть "500 Internal Server Error"
            List<WebElement> errorElements = webDriver.findElements(
                    By.xpath("//pre[contains(text(), '500 Internal Server Error')]")
            );
            if (errorElements.isEmpty()) {
                // Ошибки нет — выходим из метода
                return;
            }

            // Если элемент найден, перезагружаем страницу
            System.err.println("Страница вернула 500 Internal Server Error. Перезагружаем страницу... Попытка " + (attempt + 1));
            Thread.sleep(2000); // небольшая пауза перед refresh
            webDriver.navigate().refresh();
            attempt++;
        }

        if (attempt >= MAX_REFRESH_ATTEMPTS) {
            System.err.println("Не удалось избавиться от 500 Internal Server Error после "
                    + MAX_REFRESH_ATTEMPTS + " попыток. Продолжаем...");
        }
    }
}
