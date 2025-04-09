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
import org.example.parsercompanies.util.CaptchaSolver;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Data
@NoArgsConstructor
public class CompanyParser {

    @Autowired
    private SettingsService settingsService;


    private Long pagesDeep;
    private Double parsingDelay;
    private List<String> cities;
    private List<String> regions;
    private boolean onlyMainOKVED;
    private boolean onlyInOperation;
    private boolean partOfGovernmentProcurement;
    private boolean rememberParsingPosition;


    private Long currentPage;
    private boolean companiesParsed;
    private boolean linksOfCompaniesParsed;
    public static String logStatus = "ничего не делаем";

    private static final String INFO_FILE = Paths.get(System.getProperty("user.dir"), "cfg", "info.json").toString();
    private static final String SETTINGS_FILE = Paths.get(System.getProperty("user.dir"), "cfg", "settingsConfig.json").toString();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;
    private CaptchaSolver captchaSolver;

    private WebDriver webDriver;
    private final AtomicBoolean isParsing = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();


    private static final int WAIT_TIMEOUT_SECONDS = 30;
    @Autowired
    private CompanyRepository companyRepository;


    public void loadParsingInfo() throws IOException {

        File infoFile = new File(INFO_FILE);
        if (infoFile.exists()) {
            InfoJson info = objectMapper.readValue(infoFile, InfoJson.class);

            this.currentPage = info.getCurrentPage();
            this.companiesParsed = info.isCompaniesParsed();
            this.linksOfCompaniesParsed = info.isLinksParsed();

        } else {

            System.err.println("Info file not found: " + INFO_FILE);
            this.currentPage = 1L;
            this.companiesParsed = false;
            this.linksOfCompaniesParsed = false;
        }


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
            CaptchaSolver.anticaptchaKey = settings.getAnticaptchaKey();
        } else {
            System.err.println("Settings file not found: " + SETTINGS_FILE);

            this.pagesDeep = 1L;
            this.parsingDelay = 2.0;
            this.cities = Collections.emptyList();
            this.regions = Collections.emptyList();
        }


        if (this.companiesParsed || companyRepository.findAll().isEmpty()) {
            System.out.println("Парсинг ранее завершался, начинаем заново с 1й страницы.");
            logStatus = "Данные собраны, начинаем заново";
            this.currentPage = 1L;
            this.linksOfCompaniesParsed = false;
            companiesParsed = false;
            companyRepository.deleteAll();
        }
    }


    public void startParsing() throws IOException{

        if (isParsing.get()) {
            System.out.println("Parsing is already running.");
            return;
        }


        settingsService.reloadWebDriver();
        logStatus = "Загрузка настроек";
        System.out.println("Загрузка настроек");

        int tries = 0;
        while (!settingsService.isConfigured() && tries < 10) {
            System.out.println("Ждём, пока settingsService будет сконфигурирован...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            tries++;
        }
        if (!settingsService.isConfigured()) {
            logStatus = "Настройки так и не были загружены — завершаем.";
            System.out.println("Настройки так и не были загружены — завершаем.");
            return;
        }


        loadParsingInfo();

        companiesParsed = false;
        isParsing.set(true);
        logStatus = "Начинаем парсинг, проверка прошла";
        System.out.println("Начинаем парсинг, проверка прошла");
        System.out.println(isParsing);
        System.out.println(isCompaniesParsed());
        System.out.println(isLinksOfCompaniesParsed());
        webDriver = new ChromeDriver(
                settingsService.getOptions().addArguments("--headless")
        );
        webDriver.manage().window().maximize();
        captchaSolver = new CaptchaSolver(webDriver);
        try {

            webDriver.get("https://companium.ru/advanced/search");
            captchaSolver.isCaptchaPresent();
            if (!isLinksOfCompaniesParsed()) {
                applyFilters();
                extractAndSaveCompanyLinks();
            }


            if (linksOfCompaniesParsed && !companiesParsed && isParsing.get()) {
                parseAllCompanyLinks();
            }
        } catch (Exception e) {
            System.err.println("Error during parsing: " + e.getMessage());
            e.printStackTrace();
        } finally {

            if (webDriver != null) {
                try {
                    webDriver.quit();
                } catch (Exception ignored) {
                }
            }

            isParsing.set(false);
        }
    }


    private void applyFilters() throws IOException, InterruptedException {
        logStatus = "Применяем фильтры";
        System.out.println("Применяем фильтры");
        if (!isParsing.get()) return;
        System.out.println("Применяем чекбокс 1");
        checkAndToggleCheckbox();


        if (!isParsing.get()) return;
        System.out.println("Применяем города");
        applyCities();


        if (!isParsing.get()) return;
        System.out.println("Применяем категории");
        applyCategories();


        if (!isParsing.get()) return;
        if (partOfGovernmentProcurement) {
            System.out.println("Применяем чекбокс 2");
            checkAndTogglePartOfGovernmentProcurement();

        }
    }

    private void applyCategories() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
        Thread.sleep(2000);

        WebElement categoriesButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("input-9"))
        );
        categoriesButton.click();


        if (!isParsing.get()) return;


        List<Category> activeCategories = categoryRepository.findAllByActive(true);


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


            searchInput.sendKeys(Keys.CONTROL + "a");
            searchInput.sendKeys(Keys.DELETE);

            searchInput.sendKeys(categoryName);


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

                        }
                        break;
                    }
                } catch (NoSuchElementException ignored) {
                }
            }
        }


        WebElement selectButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("/html/body/main/div[2]/div/div[4]/div/div/div[2]/div/div/div[3]/div[2]/button")
                )
        );
        selectButton.click();

    }

    private void applyCities() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
        Thread.sleep(2000);


        wait.until(ExpectedConditions.elementToBeClickable(By.id("input-11"))).click();


        Set<String> locations = new HashSet<>();
        locations.addAll(cities);
        locations.addAll(regions);


        for (String locationName : locations) {
            if (!isParsing.get()) break;
            try {
                WebElement searchInput = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(By.id("location_search"))
                );


                searchInput.sendKeys(Keys.CONTROL + "a");
                searchInput.sendKeys(Keys.DELETE);


                searchInput.sendKeys(locationName);


                List<WebElement> results = webDriver.findElements(By.cssSelector(".v-treeview-node"));
                for (WebElement result : results) {
                    WebElement label = result.findElement(By.cssSelector(".v-treeview-node__label"));
                    String foundLocation = label.getText().trim();

                    if (foundLocation.replaceAll("\\d", "").trim().equals(locationName)) {
                        WebElement checkbox = result.findElement(By.cssSelector(".v-treeview-node__checkbox"));
                        String checkboxClass = checkbox.getAttribute("class");
                        if (!checkboxClass.contains("mdi-checkbox-marked")) {
                            checkbox.click();

                        }
                        break;
                    }
                }
                Thread.sleep((long) (parsingDelay * 1000));
            } catch (Exception e) {
                System.err.println("Error applying location filter: " + locationName + " => " + e.getMessage());
            }
        }


        WebElement selectButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("/html/body/main/div[2]/div/div[5]/div/div/div[2]/div/div/div[3]/div[2]/button")
                )
        );
        selectButton.click();

    }


    public void extractAndSaveCompanyLinks(){
        if (!isParsing.get()) return;
        logStatus = "Переходим к парсингу...";
        System.out.println("Переходим к парсингу...");
        System.out.println("Начинаем с currentPage: " + currentPage);


        if (((currentPage != null && currentPage > pagesDeep) || companyRepository.findAll().isEmpty()) && !linksOfCompaniesParsed) {
            System.out.println("currentPage (" + currentPage + ") > pagesDeep (" + pagesDeep + ") при старте. Сбрасываем на 1...");
            currentPage = 1L;
            if (rememberParsingPosition) {
                saveProgress(currentPage, false, false);
            }
        }

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));

        while (isParsing.get() && currentPage <= pagesDeep) {
            try {
                webDriver.get("https://companium.ru/advanced/search?page=" + currentPage);
                captchaSolver.isCaptchaPresent();

                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".select-section")));


                List<WebElement> rows = webDriver
                        .findElement(By.className("select-section"))
                        .findElements(By.cssSelector("table.table-lg tbody tr"));

                if (rows.isEmpty()) {
                    logStatus = "Страниц больше нет: " + currentPage;
                    this.linksOfCompaniesParsed = true;
                    if (rememberParsingPosition) {
                        saveProgress(currentPage, false, true);
                    }
                    System.out.println("No rows found on currentPage " + currentPage);
                    return;
                }


                List<String> companyLinks = new ArrayList<>();
                for (WebElement row : rows) {
                    try {
                        WebElement linkElement = row.findElement(By.cssSelector("a"));
                        String link = linkElement.getAttribute("href");
                        companyLinks.add(link);
                        System.out.println("Extracted link: " + link);
                        companyRepository.save(new Company(link));
                    } catch (Exception e) {
                        System.err.println("Error extracting link from row: " + e.getMessage());
                    }
                }

                logStatus = "Спарсена страница: " + currentPage;
                System.out.println("Page " + currentPage + " processed. Total links extracted: " + companyLinks.size());
                currentPage++;

                if (rememberParsingPosition) {
                    saveProgress(currentPage, false, false);
                }

                Thread.sleep((long) (parsingDelay * 1000));

            }







            catch (Exception e) {
                logStatus = "Ошибка при парсинге страницы: " + currentPage;
                System.err.println("Error processing currentPage " + currentPage + ": " + e.getMessage());
            }
        }


        if (currentPage > pagesDeep || isLinksOfCompaniesParsed()) {
            logStatus = "Прошли все страницы";
            System.out.println("Парсер завершил работу: прошли все страницы (currentPage=" + currentPage + " > pagesDeep=" + pagesDeep + ")");
            this.linksOfCompaniesParsed = true;
            currentPage = 1L;
            if (rememberParsingPosition) {
                saveProgress(currentPage, false, true);
            }
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
            logStatus = "Ничего не делаем";
            System.out.println("остановка парсинга");
        }
    }


    private void checkAndToggleCheckbox() throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
        Thread.sleep(2000);

        WebElement checkbox = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("input-13"))
        );


        String isChecked = checkbox.getAttribute("aria-checked");
        if ("false".equals(isChecked)) {
            WebElement label = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("label[for=\"input-13\"]")
                    )
            );
            label.click();

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


        WebElement checkbox = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("input-55"))
        );


        String isChecked = checkbox.getAttribute("aria-checked");
        if ("false".equals(isChecked)) {
            WebElement label = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.cssSelector("label[for=\"input-55\"]")
                    )
            );
            label.click();

        }
    }

    private void parseAllCompanyLinks() throws InterruptedException, IOException {
        if (!isParsing.get()) return;
        logStatus = "Начинаем парсить сами компании...";
        System.out.println("Начинаем парсить сами компании...");


        List<Company> companiesToParse = companyRepository.findAllByParsed(false);


        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));

        for (Company c : companiesToParse) {
            if (!isParsing.get()) {
                logStatus = "Ничего не делаем";
                break;
            }
            try {
                System.out.println("Открываем ссылку для парсинга: " + c.getUrl());
                webDriver.get(c.getUrl());
                captchaSolver.isCaptchaPresent();


                wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("body")));


                Thread.sleep((long) (parsingDelay * 1000));
                System.out.println("Собрали информацию по " + c.getUrl());
                Company parsedCompany = parseCompanyInfo(c);
                System.out.println(parsedCompany);
                if (parsedCompany != null) {
                    try {
                        companyRepository.save(parsedCompany);
                        System.out.println("Сохранена компания: " + parsedCompany.getUrl());
                    } catch (Exception dbException) {
                        if (dbException.getMessage().contains("A UNIQUE constraint failed")) {
                            System.err.println("Ошибка сохранения: дубликат компании " + c.getUrl());
                            companyRepository.delete(c);
                        } else {
                            throw dbException;
                        }
                    }
                }
                logStatus = "спарсена компания: " + c.getId();
            }








            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        boolean allDone = companyRepository.findAllByParsed(false).isEmpty();
        if (allDone) {
            companiesParsed = true;
            logStatus = "Парсинг закончен";
            System.out.println("Все ссылки распарсены, компанииParsed=true");
        } else {
            companiesParsed = false;
            parseAllCompanyLinks();
            logStatus = "не все ссылки обработаны";
            System.out.println("Парсер остановился, но не все ссылки обработаны");
        }
        if (rememberParsingPosition) {
            saveProgress(1L, companiesParsed, linksOfCompaniesParsed);
        }
    }

    public Company parseCompanyInfo(Company company) {
        try {
            String orgName = getTextIfPresent(By.cssSelector("#top > div > h1"));
            if (orgName != null) {
                company.setOrganizationName(orgName);
                int quoteIndex = orgName.indexOf("\"");
                if (quoteIndex > 0) {
                    company.setOrganizationType(orgName.substring(0, quoteIndex));
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге названия компании: ");
        }


        try {
            company.setRubric(getFirstActivity());
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге рода деятельности: ");
        }

        try {
            company.setFounderPosition(getDirectorPosition());
            company.setFounder(getDirectorName());
        } catch (Exception e) {
            System.err.println("Учредитель или должность не найдены: ");
        }


        try {
            company.setInn(getTextIfPresent(By.cssSelector("strong#copy-inn")));
            company.setOgrn(getTextIfPresent(By.cssSelector("strong#copy-ogrn")));
            company.setOkatoCode(getTextIfPresent(By.cssSelector("span#copy-okato-details")));
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге ИНН/ОГРН/ОКАТО: ");
        }


        try {
            String capital = findTextByLabel("Уставный капитал");
            company.setAuthorizedCapital(capital == null ? "0 руб." : capital.substring(capital.indexOf("=") + 2).trim());
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге уставного капитала: ");
        }


        try {
            String address = getTextIfPresent(By.cssSelector("span#copy-address"));
            if (address != null) {
                company.setLegalAddress(address);
                company.setCity(parseCityFromAddress(address));
            }
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге юридического адреса: ");
        }


        try {
            company.setRevenue(cleanUpPercent(getRevenue()));
            company.setProfit(cleanUpPercent(getNetProfit()));
            company.setCapital(cleanUpPercent(getCapital()));
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге финансовых данных: ");
        }


        try {
            company.setGovernmentPurchasesCustomer(getCustomerContractsSum());
            company.setGovernmentPurchasesSupplier(getSupplierContractsSum());
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге госзакупок: ");
        }

        try {
            company.setTaxes(getTaxesSum());
            company.setInsuranceContributions(getInsuranceContributionsSum());
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге налогов или страховых взносов: ");
        }


        company.setActiveCompany(true);


        try {
            company.setRegistrationDate(findTextByLabel("Дата регистрации"));
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге даты регистрации: ");
        }


        try {
            company.setNumberOfEmployees(parseNumberOfEmployees());
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге количества работников: ");
        }


        try {
            company.setOkvedCode(parseOkvedFromActivity());
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге ОКВЭД: ");
        }


        try {
            company.setPhones(parsePhones());
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге телефонов: ");
        }

        try {
            company.setEmail(parseEmailIfPresent());
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге email: ");
        }

        try {
            company.setWebsite(parseWebsiteIfPresent());
        } catch (Exception e) {
            System.err.println("Ошибка при парсинге сайта: ");
        }

        company.setParsed(true);
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
            WebElement valueElement = webDriver.findElement(By.xpath(
                    "//div[@class='fw-bold' and contains(text(),'" + labelText + "')]/following-sibling::div"
            ));
            return valueElement.getText().trim();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private String parseCityFromAddress(String address) {

        String regex = "(?<=,\\s|^)(?:г\\.?|город|г\\.о\\.|г\\. п\\.|пгт|п\\. |поселок|с\\. |село|деревня|ст-ца|мкр-н|р-н|район|аул|снт|рп|гп|х|д)\\s*([А-ЯЁа-яё\\-\\s]+?)(?=,|$)";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(address);

        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1).trim();
        }


        if (lastMatch != null &&
                !lastMatch.toLowerCase().contains("муниципальный округ") &&
                !lastMatch.toLowerCase().contains("район") &&
                !lastMatch.toLowerCase().contains("м. р-н") &&
                !lastMatch.toLowerCase().contains("р-н") &&
                !lastMatch.toLowerCase().contains("м.о.") &&
                !lastMatch.toLowerCase().contains("м. о.")) {
            return lastMatch;
        }

        return "";
    }

    public int parseNumberOfEmployees() {
        try {
            WebElement section = webDriver.findElement(By.xpath("//h2[@id='staff']/ancestor::section"));

            WebElement employeesElement = section.findElement(By.xpath(".//div[@class='money-huge']/a"));

            return Integer.parseInt(employeesElement.getText().trim().replaceAll("[^0-9]", ""));
        } catch (NoSuchElementException e) {
            return 0;
        }
    }

    private String parseOkvedFromActivity() {
        try {

            WebElement header = webDriver.findElement(By.id("activity"));


            WebElement section = header.findElement(By.xpath("./ancestor::section"));


            WebElement firstCodeElement = section.findElement(By.cssSelector("table tbody tr td"));


            return firstCodeElement.getText().trim();
        } catch (Exception e) {
            System.err.println("Ошибка при поиске кода ОКВЭД: " + e.getMessage());
            return null;
        }
    }

    public String getFirstActivity() {
        try {
            WebElement header = webDriver.findElement(By.id("activity"));

            WebElement section = header.findElement(By.xpath("./ancestor::section"));

            WebElement firstRow = section.findElement(By.cssSelector("table tbody tr"));

            WebElement activityElement = firstRow.findElement(By.cssSelector("td:nth-child(2)"));

            return activityElement.getText().replace("Основной", "").trim();
        } catch (Exception e) {
            System.err.println("Ошибка при поиске вида деятельности: " + e.getMessage());
            return "";
        }
    }

    private String parsePhones() {
        try {
            WebElement section = webDriver.findElement(By.xpath("//h2[@id='contacts']/ancestor::section"));

            WebElement phoneBlock = section.findElement(By.xpath(".//strong[contains(text(),'Телефоны')]/following-sibling::a[contains(@href, 'tel:')]"));

            List<WebElement> phoneLinks = phoneBlock.findElements(By.xpath("./ancestor::div[contains(@class, 'col-12')]/a[contains(@href, 'tel:')]"));

            if (phoneLinks.isEmpty()) {
                return null;
            }

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

    private String parseEmailIfPresent() {
        try {

            WebElement emailHeader = webDriver.findElement(By.xpath("//strong[contains(text(),'Электронная почта')]"));


            List<WebElement> emailLinks = emailHeader.findElements(By.xpath("following-sibling::a"));


            return emailLinks.stream()
                    .map(WebElement::getText)
                    .map(String::trim)
                    .filter(email -> !email.isEmpty() && email.contains("@"))
                    .collect(Collectors.joining(", "));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    private String parseWebsiteIfPresent() {
        try {

            WebElement websiteHeader = webDriver.findElement(By.xpath("//strong[contains(text(),'Веб-сайт')]"));


            List<WebElement> websiteLinks = websiteHeader.findElements(By.xpath("following-sibling::a"));


            return websiteLinks.stream()
                    .map(WebElement::getText)
                    .map(String::trim)
                    .filter(website -> !website.isEmpty())
                    .collect(Collectors.joining(", "));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getDirectorPosition() {
        try {
            WebElement section = webDriver.findElement(By.xpath("//h2[@id='management']/ancestor::section"));

            WebElement positionElement = section.findElement(By.xpath(".//strong[contains(@class, 'fw-bold')]"));

            return positionElement.getText().trim();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getDirectorName() {
        try {
            WebElement section = webDriver.findElement(By.xpath("//h2[@id='management']/ancestor::section"));

            WebElement nameElement = section.findElement(By.xpath(".//a[contains(@href, '/people/inn/')]"));

            return nameElement.getText().trim();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public String getCapital() {
        try {

            WebElement section = webDriver.findElement(By.xpath("//h2[@id='accounting']/ancestor::section"));


            WebElement capitalElement = section.findElement(By.xpath(".//strong[text()='Капитал']/following-sibling::div/a"));

            return !capitalElement.getText().trim().isBlank() ? capitalElement.getText().trim() : "0 руб.";
        } catch (NoSuchElementException e) {
            return "0 руб.";
        }
    }

    public String getNetProfit() {
        try {

            WebElement section = webDriver.findElement(By.xpath("//h2[@id='accounting']/ancestor::section"));


            WebElement profitElement = section.findElement(By.xpath(".//strong[text()='Чистая прибыль']/following-sibling::div/a"));

            return !profitElement.getText().trim().isBlank() ? profitElement.getText().trim() : "0 руб.";
        } catch (NoSuchElementException e) {
            return "0 руб.";
        }
    }

    public String getRevenue() {
        try {
            WebElement section = webDriver.findElement(By.xpath("//h2[@id='accounting']/ancestor::section"));


            WebElement revenueElement = section.findElement(By.xpath(".//strong[text()='Выручка']/following-sibling::div/a"));

            return !revenueElement.getText().trim().isBlank() ? revenueElement.getText().trim() : "0 руб.";
        } catch (NoSuchElementException e) {
            return "0 руб.";
        }
    }

    public String getSupplierContractsSum() {
        try {
            WebElement section = webDriver.findElement(By.xpath("//h2[@id='purchases']/ancestor::section"));
            WebElement sumElement = section.findElement(By.xpath(".//button[@id='contracts-pill-2']/span"));
            return sumElement.getText().trim();
        } catch (NoSuchElementException e) {
            return "0 руб.";
        }
    }

    public String getCustomerContractsSum() {
        try {
            WebElement section = webDriver.findElement(By.xpath("//h2[@id='purchases']/ancestor::section"));
            WebElement sumElement = section.findElement(By.xpath(".//button[@id='contracts-pill-1']/span"));
            return sumElement.getText().trim();
        } catch (NoSuchElementException e) {
            return "0 руб.";
        }
    }

    public String getTaxesSum() {
        return extractTaxValue("Налоги");
    }

    public String getInsuranceContributionsSum() {
        return extractTaxValue("Страховые взносы");
    }

    private String extractTaxValue(String sectionName) {
        try {
            WebElement taxesSection = webDriver.findElement(By.xpath("//h2[@id='taxes']/ancestor::section"));

            WebElement valueElement = taxesSection.findElement(By.xpath(".//div[contains(text(),'" + sectionName + "')]/following-sibling::div[contains(@class,'money-huge')]"));

            return valueElement.getText().replaceAll(".*?([\\d,\\.]+ (млн|млрд|тыс)? руб.).*", "$1").trim();
        } catch (NoSuchElementException e) {
            return "0 руб.";
        }
    }
}