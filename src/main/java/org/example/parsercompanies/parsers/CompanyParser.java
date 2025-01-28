package org.example.parsercompanies.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.parsercompanies.model.InfoJson;
import org.example.parsercompanies.model.SettingsConfig;
import org.example.parsercompanies.model.db.Category;
import org.example.parsercompanies.model.db.Company;
import org.example.parsercompanies.repos.CategoryRepository;
import org.example.parsercompanies.repos.CompanyRepository;
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

    // Основные параметры для парсинга (из settingsConfig.json)
    private Long pagesDeep;
    private Double parsingDelay;
    private List<String> cities;
    private List<String> regions;
    private boolean onlyMainOKVED;
    private boolean onlyInOperation;
    private boolean partOfGovernmentProcurement;
    private boolean rememberParsingPosition;

    // Параметры для "info.json"
    private Long currentPage;            // текущая (или последняя спарсенная) страница
    private boolean companiesParsed;     // завершён ли парсинг "компаний" целиком
    private boolean linksOfCompaniesParsed; // собраны ли все ссылки

    // Пути к JSON-файлам
    private static final String INFO_FILE = "src/main/resources/info.json";
    private static final String SETTINGS_FILE = "src/main/resources/settingsConfig.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    private WebDriver webDriver;
    private final AtomicBoolean isParsing = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Время ожидания в секундах
    private static final int WAIT_TIMEOUT_SECONDS = 30;
    @Autowired
    private CompanyRepository companyRepository;

    /**
     * Загружаем инфо о текущей странице из info.json (currentPage, companiesParsed, linksParsed)
     * + настройки из settingsConfig.json (pagesDeep, parsingDelay и т.п.).
     */
    public void loadParsingInfo() throws IOException {
        // 1) Загружаем info.json (если существует)
        File infoFile = new File(INFO_FILE);
        if (infoFile.exists()) {
            InfoJson info = objectMapper.readValue(infoFile, InfoJson.class);

            this.currentPage = info.getCurrentPage();
            this.companiesParsed = info.isCompaniesParsed();
            this.linksOfCompaniesParsed = info.isLinksParsed();

        } else {
            // Если файла нет, начинаем с 1й страницы
            System.err.println("Info file not found: " + INFO_FILE);
            this.currentPage = 1L;
            this.companiesParsed = false;
            this.linksOfCompaniesParsed = false;
        }

        // 2) Загружаем настройки из settingsConfig.json
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            SettingsConfig settings = objectMapper.readValue(settingsFile, SettingsConfig.class);
            this.pagesDeep = settings.getPagesDeep();
            this.parsingDelay = settings.getParsingDelay();
            this.cities = settings.getCities();
            this.regions = settings.getRegions();
            this.onlyMainOKVED = settings.isOnlyMainOKVED();
            this.onlyInOperation = settings.isOnlyInOperation();
            this.partOfGovernmentProcurement = settings.isPartOfGovernmentProcurement();
            this.rememberParsingPosition = settings.isRememberParsingPosition();
        } else {
            System.err.println("Settings file not found: " + SETTINGS_FILE);
            // При отсутствии настроек выставим что-то по умолчанию
            this.pagesDeep = 1L;
            this.parsingDelay = 2.0;
            this.cities = Collections.emptyList();
            this.regions = Collections.emptyList();
        }

        // Если логика проекта подразумевает "начинать заново" при уже завершённом парсинге (companiesParsed или linksParsed),
        // то можно сбросить currentPage. Например:
        if (this.companiesParsed || this.linksOfCompaniesParsed) {
            System.out.println("Парсинг ранее завершался, начинаем заново с 1й страницы.");
            this.currentPage = 1L;
            this.companiesParsed = false;
            this.linksOfCompaniesParsed = false;
        }
    }

    /**
     * Старт парсинга в отдельном потоке.
     */
    public void startParsing() throws IOException {
        if (isParsing.get()) {
            System.out.println("Parsing is already running.");
            return;
        }
        settingsService.loadSettings();
        System.out.println("Загрузка настроек");
        for (int i = 0; i <= 100; i++) {
            if(!settingsService.isConfigured()){
                continue;
            }
            if(!settingsService.isConfigured() && i >= 100){
                return;
            }
        }
        System.out.println("Начинаем парсинг, проверка прошла");
        // Ставим флаг (парсинг начат), сбрасываем companiesParsed
        companiesParsed = false;
        isParsing.set(true);

        executorService.submit(() -> {
            try {
                loadParsingInfo();

                // Инициализируем драйвер
                webDriver = new ChromeDriver(settingsService.getOptions());
                webDriver.manage().window().maximize();

                // Переходим на страницу
                webDriver.get("https://checko.ru/search/advanced");
                checkForServerErrorAndRefreshIfNeeded();

                // Применяем фильтры
                applyFilters();
                // Начинаем парсить ссылки
                System.out.println("Текущая страница: " + currentPage);
                extractAndSaveCompanyLinks();

            } catch (Exception e) {
                System.err.println("Error during parsing: " + e.getMessage());
            } finally {
                // Парсинг закончен
                companiesParsed = true;
                stopParsing();
            }
        });
    }

    private void applyFilters() throws IOException, InterruptedException {
        if (!isParsing.get()) return;

        checkAndToggleCheckbox();
        checkForServerErrorAndRefreshIfNeeded();

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

        WebElement categoriesButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("input-11"))
        );
        categoriesButton.click();
        checkForServerErrorAndRefreshIfNeeded();

        if (!isParsing.get()) return;

        // Загружаем список категорий из БД
        List<Category> activeCategories = categoryRepository.findAllByActive(true);

        // Учитывая, что некоторые названия могут содержать пробел, вырезаем часть после первого пробела
        Set<String> categoriesNames = activeCategories.stream()
                .map(category -> {
                    String name = category.getName();
                    int idx = name.indexOf(" ");
                    if (idx > 0 && idx + 1 < name.length()) {
                        return name.substring(idx + 1);
                    } else {
                        return name;
                    }
                })
                .collect(Collectors.toSet());

        for (String categoryName : categoriesNames) {
            if (!isParsing.get()) break;

            WebElement searchInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.id("activity_search"))
            );

            // Очистка (выделить и удалить)
            searchInput.sendKeys(Keys.CONTROL + "a");
            searchInput.sendKeys(Keys.DELETE);

            searchInput.sendKeys(categoryName);
            checkForServerErrorAndRefreshIfNeeded();

            // Ждём появления в дереве
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

        // Открываем список "Город"
        wait.until(ExpectedConditions.elementToBeClickable(By.id("input-13"))).click();
        checkForServerErrorAndRefreshIfNeeded();

        // Перебираем список городов из настроек
        for (String cityName : cities) {
            if (!isParsing.get()) break;
            try {
                WebElement searchInput = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(By.id("location_search"))
                );

                // Очистка
                searchInput.sendKeys(Keys.CONTROL + "a");
                searchInput.sendKeys(Keys.DELETE);

                // Вводим нужный город
                searchInput.sendKeys(cityName);
                checkForServerErrorAndRefreshIfNeeded();

                // Ждём появления в дереве
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

        // Нажимаем "Выбрать"
        WebElement selectButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("/html/body/main/div[2]/div/div[5]/div/div/div[2]/div/div/div[3]/div[2]/button")
                )
        );
        selectButton.click();
        checkForServerErrorAndRefreshIfNeeded();
    }

    /**
     * Основной цикл парсинга ссылок.
     */
    public void extractAndSaveCompanyLinks() {
        if (!isParsing.get()) return;
        System.out.println("Переходим к парсингу...");
        System.out.println("Начинаем с currentPage: " + currentPage);

        // --- ВСТАВЛЕННЫЙ БЛОК: если при старте уже currentPage > pagesDeep, сбрасываем на 1
        if (currentPage != null && currentPage > pagesDeep) {
            System.out.println("currentPage (" + currentPage + ") > pagesDeep (" + pagesDeep
                    + ") при старте. Сбрасываем на 1...");
            currentPage = 1L;
            if (rememberParsingPosition) {
                // Сохраняем, чтобы в info.json теперь было currentPage=1
                saveProgress(currentPage, false, false);
            }
        }
        // --- конец вставленного блока

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));

        while (isParsing.get() && currentPage <= pagesDeep) {
            try {
                webDriver.get("https://checko.ru/search/advanced?page=" + currentPage);
                checkForServerErrorAndRefreshIfNeeded();

                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("main")));
                checkForServerErrorAndRefreshIfNeeded();

                // Проверяем, нет ли сообщения "Не найдено ни одного юридического лица"
                List<WebElement> notFoundElements = webDriver.findElements(
                        By.xpath("/html/body/main/div[2]/div/div[2]/div/p")
                );
                if (!notFoundElements.isEmpty()
                        && notFoundElements.get(0).getText().contains("Не найдено ни одного юридического лица")) {
                    System.out.println("No more companies found. Stopping pagination.");
                    break;
                }

                // Ищем строки в таблице
                List<WebElement> rows = webDriver
                        .findElement(By.className("select-section"))
                        .findElements(By.cssSelector("table.table-lg tbody tr"));

                if (rows.isEmpty()) {
                    System.out.println("No rows found on currentPage " + currentPage);
                    break;
                }

                // Собираем ссылки
                List<String> companyLinks = new ArrayList<>();
                for (WebElement row : rows) {
                    try {
                        WebElement linkElement = row.findElement(By.cssSelector("a.link"));
                        String link = linkElement.getAttribute("href");
                        companyLinks.add(link);
                        System.out.println("Extracted link: " + link);
                        // Здесь можно сохранить в БД, если нужно
                        companyRepository.save(new Company(link));
                    } catch (Exception e) {
                        System.err.println("Error extracting link from row: " + e.getMessage());
                    }

                    checkForServerErrorAndRefreshIfNeeded();
                }

                System.out.println("Page " + currentPage + " processed. Total links extracted: " + companyLinks.size());
                currentPage++;

                // Если нужно запоминать позицию — сохраняем прогресс
                if (rememberParsingPosition) {
                    saveProgress(currentPage, false, false);
                }

                Thread.sleep((long) (parsingDelay * 1000));

            } catch (Exception e) {
                System.err.println("Error processing currentPage " + currentPage + ": " + e.getMessage());
                break;
            }
        }

        // Если дошли сюда и currentPage > pagesDeep => значит все ссылки по страницам собраны
        if (currentPage > pagesDeep) {
            System.out.println("Парсер завершил работу: прошли все страницы (currentPage="
                    + currentPage + " > pagesDeep=" + pagesDeep + ")");
            this.linksOfCompaniesParsed = true;

            // Ставим currentPage на 1, чтобы в следующий раз начинать заново
            currentPage = 1L;
            // Записываем финальное состояние
            if (rememberParsingPosition) {
                saveProgress(currentPage, false, true);
            }
        } else {
            // Если вышли по break, всё равно считаем, что ссылки собраны (или прерываемся)
            this.linksOfCompaniesParsed = false;
            if (rememberParsingPosition) {
                saveProgress(currentPage, false, false);
            }
        }
    }

    /**
     * Сохраняем текущее состояние парсинга (currentPage, companiesParsed, linksParsed)
     * в info.json — именно то, что нужно по условию.
     */
    private void saveProgress(Long currentPage, boolean isCompleted, boolean isLinksParsed) {
        try {
            InfoJson infoJson = new InfoJson();
            infoJson.setCurrentPage(currentPage);
            infoJson.setCompaniesParsed(isCompleted);
            infoJson.setLinksParsed(isLinksParsed);

            // Записываем в файл info.json
            objectMapper.writeValue(new File(INFO_FILE), infoJson);
        } catch (IOException e) {
            System.err.println("Error saving progress: " + e.getMessage());
        }
    }

    /**
     * Завершение парсинга, остановка драйвера.
     */
    public void stopParsing() {
        isParsing.set(false);
        if (webDriver != null) {
            webDriver.quit();
        }
    }

    /**
     * "Только действующие" чекбокс (id="input-9").
     */
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
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("label[for=\"input-9\"]")
                    )
            );
            label.click();
            checkForServerErrorAndRefreshIfNeeded();
        }
    }

    /**
     * "Участники госзакупок" чекбокс (id="input-55"), открывается при клике
     * на xpath "/html/body/main/div[2]/div/div[1]/div/div/div/div[6]/div[7]/strong/button"
     */
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
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("label[for=\"input-55\"]")
                    )
            );
            label.click();
            checkForServerErrorAndRefreshIfNeeded();
        }
    }

    /**
     * Проверяем, не появился ли на странице текст "500 Internal Server Error".
     * Если появился, несколько раз перезагружаем страницу, пока ошибка не пропадёт.
     */
    private void checkForServerErrorAndRefreshIfNeeded() throws InterruptedException {
        final int MAX_REFRESH_ATTEMPTS = 5;
        int attempt = 0;

        while (attempt < MAX_REFRESH_ATTEMPTS && isParsing.get()) {
            // Ищем элемент <pre>, который содержит "500 Internal Server Error"
            List<WebElement> errorElements = webDriver.findElements(
                    By.xpath("//pre[contains(text(), '500 Internal Server Error')]")
            );
            if (errorElements.isEmpty()) {
                // Если нет — выходим
                return;
            }

            // Если элемент найден — делаем refresh
            System.err.println("Страница вернула 500 Internal Server Error. Перезагружаем. Попытка " + (attempt + 1));
            Thread.sleep(2000);
            webDriver.navigate().refresh();
            attempt++;
        }

        if (attempt >= MAX_REFRESH_ATTEMPTS) {
            System.err.println("Не удалось избавиться от 500 Internal Server Error за "
                    + MAX_REFRESH_ATTEMPTS + " попыток. Продолжаем...");
        }
    }
}
