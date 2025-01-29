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
        // Укажите путь к вашему chromedriver

        // Откройте страницу с деревом категорий
        driver.get("https://checko.ru/search/advanced");

        WebElement categoriesOpenButton = driver.findElement(By.id("input-11"));
        categoriesOpenButton.click();
        // Найдите корневой контейнер дерева
        WebElement treeContainer = driver.findElement(By.cssSelector(".v-treeview"));

        // Развернуть все узлы и собрать категории
        List<String> categories = new ArrayList<>();
        expandAndParseTree(treeContainer, categories);

        // Вывод категорий
        for (String category : categories) {
            categoryRepository.save(new Category(category,false,getCategoryLevel(category)));
        }
        categoriesParsed = true;
        driver.quit();
    }

    private static void expandAndParseTree(WebElement treeContainer, List<String> categories) {
        // Найти все узлы дерева
        List<WebElement> nodes = treeContainer.findElements(By.cssSelector(".v-treeview-node"));

        for (WebElement node : nodes) {
            // Развернуть узел, если он свернут
            WebElement toggleButton = null;
            try {
                toggleButton = node.findElement(By.cssSelector(".v-treeview-node__toggle"));
                if (toggleButton.getAttribute("class").contains("mdi-plus")) {
                    toggleButton.click();
                    // Дождаться рендеринга новых узлов (при необходимости)
                    Thread.sleep((Long) settingsService.getSettings().get("parsingDelay")); // Увеличьте, если требуется больше времени
                }
            } catch (Exception ignored) {
                // Узел может быть листом или уже раскрыт
            }

            // Извлечь название категории
            try {
                WebElement label = node.findElement(By.cssSelector(".v-treeview-node__label"));
                String categoryName = label.getText().trim();
                categories.add(categoryName);
            } catch (Exception ignored) {
                // Если элемент не найден, пропускаем
            }

            // Рекурсивно обработать дочерние узлы
            try {
                WebElement childrenContainer = node.findElement(By.cssSelector(".v-treeview-node__children"));
                expandAndParseTree(childrenContainer, categories);
            } catch (Exception ignored) {
                // Если дочерние узлы отсутствуют, пропускаем
            }
        }
    }

    public static int getCategoryLevel(String categoryText) {
        if (categoryText == null || categoryText.isEmpty()) {
            return 1; // Возвращаем 1, если текст пустой
        }

        // Если начинается с буквы, это первый уровень
        if (categoryText.matches("^[A-Za-zА-Яа-я].*")) {
            return 1;
        }

        // Если начинается с цифры, уровень определяется по количеству точек
        if (categoryText.matches("^\\d+(\\.\\d+)*\\s.*")) {
            String numericPart = categoryText.split("\\s")[0]; // Извлекаем первую часть
            return numericPart.split("\\.").length; // Уровень определяется количеством точек + 1
        }

        // Если ничего не подходит, возвращаем 1 по умолчанию
        return 1;
    }
}
//парсить категории будет с меню фильтров в /advanced