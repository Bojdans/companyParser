package org.example.parsercompanies.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.example.parsercompanies.model.InfoJson;
import org.example.parsercompanies.model.db.Category;
import org.example.parsercompanies.repos.CategoryRepository;
import org.example.parsercompanies.services.SettingsService;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class CategoriesParser {
    private static SettingsService settingsService;
    private CategoryRepository categoryRepository;
    @Getter
    private boolean categoriesParsed = false;
    private static final String INFO_FILE = Paths.get(System.getProperty("user.dir"), "cfg", "info.json").toString();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    public CategoriesParser(SettingsService settingsService, CategoryRepository categoryRepository) {
        this.settingsService = settingsService;
        this.categoryRepository = categoryRepository;
    }

    public void startParsing() throws IOException, InterruptedException {
        categoriesParsed = false;
        categoryRepository.deleteAll();

        WebDriver driver = new ChromeDriver(settingsService.getOptions().addArguments("--headless"));
        driver.manage().window().maximize();
        

        
        driver.get("https://checko.ru/search/advanced");

        WebElement categoriesOpenButton = driver.findElement(By.id("input-11"));
        categoriesOpenButton.click();
        
        WebElement treeContainer = driver.findElement(By.cssSelector(".v-treeview"));

        
        List<String> categories = new ArrayList<>();
        expandAndParseTree(treeContainer, categories);

        
        for (String category : categories) {
            categoryRepository.save(new Category(category,false,getCategoryLevel(category)));
        }
        categoriesParsed = true;
        driver.quit();
    }

    private static void expandAndParseTree(WebElement treeContainer, List<String> categories) {
        
        List<WebElement> nodes = treeContainer.findElements(By.cssSelector(".v-treeview-node"));

        for (WebElement node : nodes) {
            
            WebElement toggleButton = null;
            try {
                toggleButton = node.findElement(By.cssSelector(".v-treeview-node__toggle"));
                if (toggleButton.getAttribute("class").contains("mdi-plus")) {
                    toggleButton.click();
                    
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
