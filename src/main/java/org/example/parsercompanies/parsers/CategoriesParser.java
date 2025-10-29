package org.example.parsercompanies.parsers;

import lombok.Getter;
import org.example.parsercompanies.model.db.Category;
import org.example.parsercompanies.repos.CategoryRepository;
import org.example.parsercompanies.services.SettingsService;
import org.example.parsercompanies.util.CaptchaSolver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
@Component
public class CategoriesParser {
    private static final long WAIT_TIMEOUT_SECONDS = 30;
    private static SettingsService settingsService;
    private CategoryRepository categoryRepository;
    private CaptchaSolver captchaSolver;
    @Getter
    private boolean categoriesParsed = false;
    @Autowired
    public CategoriesParser(SettingsService settingsService, CategoryRepository categoryRepository) {
        this.settingsService = settingsService;
        this.categoryRepository = categoryRepository;
    }

    public void startParsing() throws IOException, InterruptedException {
        categoriesParsed = false;
        categoryRepository.deleteAll();

//        WebDriver driver = new ChromeDriver(.addArguments("--headless"));
        WebDriver driver = new ChromeDriver(settingsService.getOptions());
        captchaSolver = new CaptchaSolver(driver);
//        driver.manage().window().maximize();
        driver.manage().window().setSize(new Dimension(1920, 1080));
        CompanyParser.logStatus = "Открываем страницу для парсинга";
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
        driver.get("https://companium.ru/advanced/search");
        captchaSolver.isCaptchaPresent();
        WebElement categoriesButton = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("input-9"))
        );
        categoriesButton.click();
        captchaSolver.isCaptchaPresent();
        WebElement treeContainer = driver.findElement(By.cssSelector(".v-treeview"));

        
        List<String> categories = new ArrayList<>();
        expandAndParseTree(treeContainer, categories);

        
        for (String category : categories) {
            categoryRepository.save(new Category(category,false,getCategoryLevel(category)));
        }
        categoriesParsed = true;
        CompanyParser.logStatus = "Спарсены все ОКВЭД";
        System.out.println("Спарсены все ОКВЭД");
        driver.quit();
    }

    private void expandAndParseTree(WebElement treeContainer, List<String> categories) {
        
        List<WebElement> nodes = treeContainer.findElements(By.cssSelector(".v-treeview-node"));
        CompanyParser.logStatus = "Раскрываем дерево ОКВЭД";
        System.out.println("Раскрываем дерево ОКВЭД");
        for (WebElement node : nodes) {
            WebElement toggleButton;
            try {
                toggleButton = node.findElement(By.cssSelector(".v-treeview-node__toggle"));
                if (toggleButton.getAttribute("class").contains("mdi-plus")) {
                    toggleButton.click();
                    captchaSolver.isCaptchaPresent();
                    Thread.sleep((Long) settingsService.getSettings().get("parsingDelay")); 
                }
            } catch (Exception ignored) {
                
            }

            
            try {
                WebElement label = node.findElement(By.cssSelector(".v-treeview-node__label"));
                String categoryName = label.getText().trim();
                categories.add(categoryName);
            } catch (Exception ignored) {
                
            }

            
            try {
                WebElement childrenContainer = node.findElement(By.cssSelector(".v-treeview-node__children"));
                expandAndParseTree(childrenContainer, categories);
            } catch (Exception ignored) {
                
            }
        }
    }

    public static int getCategoryLevel(String categoryText) {
        if (categoryText == null || categoryText.isEmpty()) {
            return 1; 
        }

        
        if (categoryText.matches("^[A-Za-zА-Яа-я].*")) {
            return 1;
        }

        
        if (categoryText.matches("^\\d+(\\.\\d+)*\\s.*")) {
            String numericPart = categoryText.split("\\s")[0]; 
            return numericPart.split("\\.").length; 
        }

        
        return 1;
    }
}
