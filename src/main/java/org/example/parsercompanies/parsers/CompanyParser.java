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
import java.math.BigDecimal;
import java.nio.file.Paths;
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
    private String logStatus;
    // Пути к JSON-файлам
    private static final String INFO_FILE = Paths.get(System.getProperty("user.dir"), "cfg", "info.json").toString();
    private static final String SETTINGS_FILE = Paths.get(System.getProperty("user.dir"), "cfg", "settingsConfig.json").toString();

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
        if (this.companiesParsed || companyRepository.findAll().isEmpty()) {
            System.out.println("Парсинг ранее завершался, начинаем заново с 1й страницы.");
            logStatus = "Данные собраны, начинаем заново";
            this.currentPage = 1L;
            this.linksOfCompaniesParsed = false;
            companiesParsed = false;
            companyRepository.deleteAll();
        }
    }

    public static void killAllChromeAndDrivers() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                // Windows: taskkill
                // /F — принудительно, /IM — по имени процесса, /T — убивает дерево процессов
                Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe /T");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Старт парсинга в отдельном потоке.
     */
    public void startParsing() throws IOException {
        // 1. Проверяем, не идёт ли уже парсинг
        if (isParsing.get()) {
            System.out.println("Parsing is already running.");
            return;
        }

        // 2. Загружаем настройки
        settingsService.reloadWebDriver();
        logStatus = "Загрузка настроек";
        System.out.println("Загрузка настроек");

        // 3. Делаем разумную попытку дождаться, пока сервис настроек будет сконфигурирован
        //    (если нужно действительно подождать некоторое время, а не просто "пропустить")
        int tries = 0;
        while (!settingsService.isConfigured() && tries < 10) {
            System.out.println("Ждём, пока settingsService будет сконфигурирован...");
            try {
                Thread.sleep(1000); // ждём 1 секунду
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            tries++;
        }
        // Если после 10 попыток всё ещё не настроено — уходим
        if (!settingsService.isConfigured()) {
            logStatus = "Настройки так и не были загружены — завершаем.";
            System.out.println("Настройки так и не были загружены — завершаем.");
            return;
        }
        // Дополнительно, если loadParsingInfo() должна выполняться
        // только 1 раз и не блокирует основной поток — можно оставить здесь
        loadParsingInfo();

        companiesParsed = false;
        isParsing.set(true);
        logStatus = "Начинаем парсинг, проверка прошла";
        System.out.println("Начинаем парсинг, проверка прошла");
        System.out.println(isParsing);
        System.out.println(isCompaniesParsed());
        System.out.println(isLinksOfCompaniesParsed());
        webDriver = new ChromeDriver(
                settingsService.getOptions().addArguments("--headless=new")
        );
        webDriver.manage().window().maximize();

        try {
            // 5.2 Переходим на страницу
            webDriver.get("https://checko.ru/search/advanced");
            checkForServerErrorAndRefreshIfNeeded();

            // 5.3 Применяем фильтры

            if (!isLinksOfCompaniesParsed()) {
                applyFilters();
                // 5.4 Парсим ссылки
                extractAndSaveCompanyLinks();
            }

            // Если ссылки собраны, но данные ещё не распарсены, продолжаем
            if (linksOfCompaniesParsed && !companiesParsed && isParsing.get()) {
                parseAllCompanyLinks();
            }
        } catch (Exception e) {
            System.err.println("Error during parsing: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 5.5 Всегда закрываем драйвер, если он был инициализирован
            if (webDriver != null) {
                try {
                    webDriver.quit();
                } catch (Exception ignored) {
                }
            }
            logStatus = "Парсинг закончен";
            // Ставим флаг, что парсинг завершён
            companiesParsed = true;

            // Снимаем признак "парсинг идёт"
            isParsing.set(false);
        }
    }


    private void applyFilters() throws IOException, InterruptedException {
        logStatus = "Применяем фильтры";
        System.out.println("Применяем фильтры");
        if (!isParsing.get()) return;
        System.out.println("Применяем чекбокс 1");
        checkAndToggleCheckbox();
        checkForServerErrorAndRefreshIfNeeded();

        if (!isParsing.get()) return;
        System.out.println("Применяем города");
        applyCities();
        checkForServerErrorAndRefreshIfNeeded();

        if (!isParsing.get()) return;
        System.out.println("Применяем категории");
        applyCategories();
        checkForServerErrorAndRefreshIfNeeded();

        if (!isParsing.get()) return;
        if (partOfGovernmentProcurement) {
            System.out.println("Применяем чекбокс 1");
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
        logStatus = "Переходим к парсингу...";
        System.out.println("Переходим к парсингу...");
        System.out.println("Начинаем с currentPage: " + currentPage);

        // --- ВСТАВЛЕННЫЙ БЛОК: если при старте уже currentPage > pagesDeep, сбрасываем на 1
        if (((currentPage != null && currentPage > pagesDeep) || companyRepository.findAll().isEmpty()) && !linksOfCompaniesParsed) {
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
            logStatus = "Прошли все страницы";
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
            logStatus = "Парсер выключен";
            System.out.println("остановка парсинга");
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

    private void parseAllCompanyLinks() {
        if (!isParsing.get()) return;
        logStatus = "Начинаем парсить сами компании...";
        System.out.println("Начинаем парсить сами компании...");

        // Выберем все Company, у которых parsed = false
        List<Company> companiesToParse = companyRepository.findAllByParsed(false);
        // Если репозиторий не поддерживает такой метод —
        // нужно создать запрос вручную или обойтись findAll().
        // Ниже предполагается, что метод findAllByParsedFalse() существует.

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));

        for (Company c : companiesToParse) {
            if (!isParsing.get()) {
                // если парсер остановлен, прекращаем
                break;
            }
            try {
                System.out.println("Открываем ссылку для парсинга: " + c.getUrl());
                webDriver.get(c.getUrl());
                checkForServerErrorAndRefreshIfNeeded();

                // Ждём, пока страница прогрузится (условно)
                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));

                // Здесь ВАША логика сбора информации (stub):
                // parseSingleCompanyPage(); // Сбор нужных данных
                // Пример заглушки:
                Thread.sleep((long) (parsingDelay * 1000));
                System.out.println("Собрали информацию по " + c.getUrl());
                System.out.println(parseCompanyInfo(c));
                companyRepository.save(parseCompanyInfo(c));

            } catch (Exception e) {
                System.err.println("Ошибка при парсинге ссылки " + c.getUrl() + ": " + e.getMessage());
            }
        }

        // Проверим, все ли ссылки теперь parsed
        boolean allDone = companyRepository.findAllByParsed(false).isEmpty();
        if (allDone) {
            // Успешно прошли по всем компаниям
            companiesParsed = true;
            logStatus = "Парсинг закончен";
            System.out.println("Все ссылки распарсены, компанииParsed=true");
        } else {
            // Прервались до конца
            companiesParsed = false;
            System.out.println("Парсер остановился, но не все ссылки обработаны");
        }

        // Сохраним финальное состояние
        if (rememberParsingPosition) {
            saveProgress(1L, companiesParsed, linksOfCompaniesParsed);
        }
    }


    //парсинг одной компании

    public Company parseCompanyInfo(Company company) {
        // Название организации (пример: <h1 id="cn">ООО КОМБИНАТ "ДУБКИ"</h1>) +
        company.setOrganizationName(getTextIfPresent(By.cssSelector("h1#cn")));
        company.setOrganizationType(getTextIfPresent(By.cssSelector("h1#cn")).substring(0, getTextIfPresent(By.cssSelector("h1#cn")).indexOf("\"")));


        //учредитель и должность +
        try {
            WebElement infoBlock = webDriver.findElement(
                    By.cssSelector("div.d-flex div.flex-grow-1.ms-3")
            );
            // «Должность» (например: «Генеральный директор», «Владелец», «Учредитель»)
            WebElement positionElement = infoBlock.findElement(By.cssSelector("div.fw-700"));
            String position = positionElement.getText().trim();
            company.setFounderPosition(position);

            // ФИО
            WebElement nameElement = infoBlock.findElement(By.cssSelector("a.link"));
            String name = nameElement.getText().trim();
            company.setFounder(name);

        } catch (NoSuchElementException e) {
            // Если блока нет, ставим null
            company.setFounderPosition(null);
            company.setFounder(null);
        }

        // ИНН +
        company.setInn(getTextIfPresent(By.cssSelector("strong#copy-inn")));
        // ОГРН +
        company.setOgrn(getTextIfPresent(By.cssSelector("strong#copy-ogrn")));
        // ОКАТО +
        company.setOkatoCode(getTextIfPresent(By.cssSelector("span#copy-okato")));

        // Уставный капитал (ищем текст после "Уставный капитал") +
        company.setAuthorizedCapital(findTextByLabel("Уставный капитал"));

        // Юридический адрес + город +
        String address = getTextIfPresent(By.cssSelector("span#copy-address"));
        company.setLegalAddress(address);
        company.setCity(parseCityFromAddress(address));
        //выручка прибыль капитал
        try {
            // Ищем блок <div id="accounting-huge">
            WebElement accountingBlock = webDriver.findElement(By.cssSelector("div#accounting-huge"));

            // Находим все вложенные колонки: div.col-12.col-md-4
            List<WebElement> columns = accountingBlock.findElements(By.cssSelector("div.col-12.col-md-4"));

            // 1) Выручка (из первой колонки)
            //    Ищем div.text-huge и достаём текст, например: "23 млн руб. -15%"
            WebElement revenueBlock = columns.get(0).findElement(By.cssSelector("div.text-huge"));
            String revenueText = revenueBlock.getText().trim();
            // Удалим возможный процент роста/спада и парсим число
            company.setRevenue(cleanUpPercent(revenueText));

            // 2) Прибыль (вторая колонка)
            WebElement profitBlock = columns.get(1).findElement(By.cssSelector("div.text-huge"));
            String profitText = profitBlock.getText().trim();
            company.setProfit(cleanUpPercent(profitText));

            // 3) Капитал (третья колонка)
            WebElement capitalBlock = columns.get(2).findElement(By.cssSelector("div.text-huge"));
            String capitalText = capitalBlock.getText().trim();
            company.setCapital(cleanUpPercent(capitalText));

        } catch (NoSuchElementException e) {
            // Если блок или нужные элементы не найдены, не ломаем код
        }
        //госзакупки заказчик -
        //госзакупки поставщик -
        try {
            // 1) Ищем сам блок <section id="contracts">, чтобы ограничить поиск
            WebElement contractsSection = webDriver.findElement(By.cssSelector("section#contracts"));

            // 2) Внутри него ищем строку: div.row.gy-3.gx-4.mb-4
            WebElement row = contractsSection.findElement(By.cssSelector("div.row.gy-3.gx-4.mb-4"));

            // --- ЗАказчик ---
            // <div class="col-12 col-md-6 col-xxl-4"> ... </div>
            WebElement customerColumn = row.findElement(By.cssSelector("div.col-12.col-md-6.col-xxl-4"));
            // Внутри ищем .text-huge -> например: "0 руб." или "123,4 тыс. руб."
            WebElement customerBlock = customerColumn.findElement(By.cssSelector("div.text-huge"));
            String customerText = customerBlock.getText().trim();
            company.setGovernmentPurchasesCustomer(customerText);

            // --- Поставщик ---
            // <div class="col-12 col-md-6 col-xxl-8"> ... </div>
            WebElement supplierColumn = row.findElement(By.cssSelector("div.col-12.col-md-6.col-xxl-8"));
            WebElement supplierBlock = supplierColumn.findElement(By.cssSelector("div.text-huge"));
            String supplierText = supplierBlock.getText().trim();
            company.setGovernmentPurchasesSupplier(supplierText);

        } catch (NoSuchElementException e) {
            // Если элементы не найдены — пусть поля останутся null или 0
        }
        // Страховые взносы и налоги +
        try {
            // Основной блок, где есть строки «Налоги» и «Страховые взносы»:
            WebElement taxesSection = webDriver.findElement(By.cssSelector("section#taxes"));

            // Теперь ищем div.row.gy-3.gx-4 только внутри найденного section#taxes
            WebElement row = taxesSection.findElement(By.cssSelector("div.row.gy-3.gx-4"));

            // 1) Налоги
            // div.col-12.col-md-6.col-xxl-4 => внутри него:
            // <div class="mb-2 fw-700">Налоги</div>
            // <div class="text-huge mb-1">...<a>237,3 <span>млн руб.</span></a>...</div>
            WebElement taxesColumn = row.findElement(
                    By.cssSelector("div.col-12.col-md-6.col-xxl-4")
            );
            // Берём текст из блока "div.text-huge.mb-1"
            WebElement taxesBlock = taxesColumn.findElement(
                    By.cssSelector("div.text-huge.mb-1")
            );
            String taxesText = taxesBlock.getText().trim();
            company.setTaxes(cleanUpPercent(taxesText));

            // 2) Страховые взносы
            // div.col-12.col-md-6.col-xxl-8 => аналогично
            WebElement insuranceColumn = row.findElement(
                    By.cssSelector("div.col-12.col-md-6.col-xxl-8")
            );
            WebElement insuranceBlock = insuranceColumn.findElement(
                    By.cssSelector("div.text-huge.mb-1")
            );
            String insuranceText = insuranceBlock.getText().trim();
            company.setInsuranceContributions(cleanUpPercent(insuranceText));

        }
        catch (NoSuchElementException e) {
        }

        company.setActiveCompany(true);

        // Дата регистрации +
        company.setRegistrationDate(findTextByLabel("Дата регистрации"));

        // Количество работников +
        company.setNumberOfEmployees(
                parseNumberOfEmployees()
        );

        // ОКВЭД +
        company.setOkvedCode(parseOkvedFromActivity());

        //парсим телефон и почту +
        company.setPhones(parsePhones());
        company.setEmail(parseEmailIfPresent());
        company.setWebsite(parseWebsiteIfPresent());
        return company;
    }
    private String cleanUpPercent(String text) {
        return text.replaceAll("(\\+|-)\\d+%?$", "").trim();
    }

    private String getTextIfPresent(By selector) {
        try {
            return webDriver.findElement(selector).getText().trim();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private String findTextByLabel(String labelText) {
        try {
            // Ищем div, у которого class="fw-700" и содержится text() с labelText,
            // а затем берем его следующий sibling <div>.
            WebElement valueElement = webDriver.findElement(By.xpath(
                    "//div[@class='fw-700' and contains(text(),'" + labelText + "')]/following-sibling::div"
            ));
            return valueElement.getText().trim();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private String parseCityFromAddress(String address) {
        if (address == null) return null;
        String lower = address.toLowerCase();
        int idx = lower.indexOf("г. ");
        if (idx >= 0) {
            int start = idx + 3;
            int end = lower.indexOf(",", start);
            if (end == -1) end = address.length();
            return address.substring(start, end).trim();
        }
        return null;
    }

    private Integer parseNumberOfEmployees() {
        String digits = webDriver.findElement(By.xpath("/html/body/main/div[2]/div/article/div/div[4]/div[2]/div[3]/div[2]")).getText().replaceAll("\\D+", "");
        if (digits.isEmpty()) return null;
        return Integer.valueOf(digits);
    }

    private String parseOkvedFromActivity() {
        String activity = findTextByLabel("Вид деятельности");
        if (activity == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\(([^)]+)\\)").matcher(activity);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }


    private String parsePhones() {
        try {
            // Ищем все элементы <a> где href начинается с "tel:"
            // Они могут быть в колонке "Телефоны"
            WebElement phoneBlock = webDriver.findElement(By.xpath(
                    "//strong[@class='fw-700 d-block mb-1' and contains(text(),'Телефоны')]/parent::div"
            ));
            // Внутри phoneBlock ищем все <a href="tel:...">
            var phoneLinks = phoneBlock.findElements(By.cssSelector("a[href^='tel:']"));

            if (phoneLinks.isEmpty()) {
                return null;
            }
            // Собираем
            StringBuilder sb = new StringBuilder();
            for (WebElement link : phoneLinks) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(link.getText().trim());
            }
            return sb.toString();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Ищем Email (в блоке "Электронная почта").
     */
    private String parseEmailIfPresent() {
        try {
            // Ищем элемент <strong> с текстом "Электронная почта",
            // затем берём его первый соседний элемент <a> (following-sibling::a[1]).
            WebElement emailLink = webDriver.findElement(By.xpath(
                    "//strong[contains(text(),'Электронная почта')]/following-sibling::a[1]"
            ));
            return emailLink.getText().trim();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
    private String parseWebsiteIfPresent() {
        try {
            // Ищем элемент <strong> с текстом "Веб-сайт",
            // затем берём его первый соседний элемент <a> (following-sibling::a[1]).
            WebElement websiteLink = webDriver.findElement(By.xpath(
                    "//strong[contains(text(),'Веб-сайт')]/following-sibling::a[1]"
            ));
            return websiteLink.getText().trim();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}

