package org.example.parsercompanies.util;

import org.example.parsercompanies.parsers.CompanyParser;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CaptchaSolver {

    private final WebDriver driver;
    public static String anticaptchaKey = "f2f8da96489967ea555d00476eff18ee";

    public CaptchaSolver(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isCaptchaPresent() {
        try {
            String title = driver.getTitle().toLowerCase();
            if (title.contains("подтвердите") || title.contains("вы человек")) {
                System.out.println("[INFO] Обнаружена капча по заголовку страницы. Запуск решения...");
                CompanyParser.logStatus = "решаем капчу";
                solveCaptcha();
                return true;
            }
            System.out.println("[INFO] Капча не обнаружена по заголовку.");
            return false;

        } catch (Exception e) {
            System.out.println("[ERROR] Ошибка при анализе title страницы: " + e.getMessage());
            return false;
        }
    }
    public void solveCaptcha() throws Exception {
        System.out.println("[INFO] Поиск ключа сайта и URL страницы...");
        String siteKey = driver.findElement(By.className("g-recaptcha")).getAttribute("data-sitekey");
        String pageUrl = driver.getCurrentUrl();

        System.out.println("[INFO] Ключ сайта: " + siteKey);
        System.out.println("[INFO] Отправка задачи на Antigate...");
        String postData = "{" +
                "\"clientKey\": \"" + anticaptchaKey + "\"," +
                "\"task\": {\"type\": \"NoCaptchaTaskProxyless\",\"websiteURL\": \"" + pageUrl + "\",\"websiteKey\": \"" + siteKey + "\"}" +
                "}";

        String taskId = sendAntigateTask("https://api.anti-captcha.com/createTask", postData);
        System.out.println("[INFO] Получен taskId: " + taskId);

        System.out.println("[INFO] Ожидание результата...");
        String token = getAntigateResult(taskId);

        System.out.println("[INFO] Вставка токена в страницу...");
        ((JavascriptExecutor) driver).executeScript("document.getElementById('g-recaptcha-response').style.display = 'block';");
        WebElement response = driver.findElement(By.id("g-recaptcha-response"));
        response.sendKeys(token);
        System.out.println("[SUCCESS] Капча успешно решена и вставлена.");
        System.out.println("[INFO] Нажатие на кнопку отправки формы...");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
    }

    private String sendAntigateTask(String requestUrl, String json) throws Exception {
        URL url = new URL(requestUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setDoOutput(true);
        con.getOutputStream().write(json.getBytes("UTF-8"));

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();

        String response = sb.toString();
        System.out.println("[DEBUG] Ответ на создание задачи: " + response);
        if (response.contains("taskId")) {
            return response.split("\\\"taskId\\\":")[1].replaceAll("[^0-9]", "");
        } else {
            throw new RuntimeException("Antigate task creation failed: " + response);
        }
    }

    private String getAntigateResult(String taskId) throws Exception {
        String resultUrl = "https://api.anti-captcha.com/getTaskResult";
        String json = "{\"clientKey\": \"" + anticaptchaKey + "\", \"taskId\": " + taskId + "}";

        while (true) {
            Thread.sleep(5000);
            System.out.println("[INFO] Запрос статуса решения... taskId: " + taskId);

            URL url = new URL(resultUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setDoOutput(true);
            con.getOutputStream().write(json.getBytes("UTF-8"));

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            in.close();

            String response = sb.toString();
            System.out.println("[DEBUG] Ответ от Antigate: " + response);
            if (response.contains("ready") && response.contains("solution")) {
                return response.split("\\\"gRecaptchaResponse\\\":\\\"")[1].split("\\\"")[0];
            } else if (!response.contains("processing")) {
                throw new RuntimeException("Antigate response error: " + response);
            }
        }
    }

}