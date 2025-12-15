package org.example.parsercompanies.util;

import org.example.parsercompanies.parsers.CompanyParser;
import org.example.parsercompanies.services.SettingsService;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CaptchaSolver {

    private final WebDriver driver;
    SettingsService settingsService;
    public static String anticaptchaKey = "f2f8da96489967ea555d00476eff18ee";
    public static String rucaptchaKey = "8cb4a0236e2a38fcdf1f0d60f56f3e05";
    public static boolean yandexCaptcha = false;
    public static boolean googleCaptcha = true;
    public CaptchaSolver(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isCaptchaPresent() {
        try {
            String title = driver.getTitle().toLowerCase();
            if (title.contains("подтвердите") || title.contains("вы человек")) {
                System.out.println("[INFO] Обнаружена капча по заголовку страницы. Запуск решения...");
                CompanyParser.logStatus = "решаем капчу";
                if (yandexCaptcha) {
                    solveSmartCaptchaWithRuCaptcha();
                }
                if (googleCaptcha) {
                    solveReCaptcha();
                }
                return true;
            }
            System.out.println("[INFO] Капча не обнаружена по заголовку.");
            return false;

        } catch (Exception e) {
            System.out.println("[ERROR] Ошибка при анализе title страницы: " + e.getMessage());
            return false;
        }
    }
    public void solveReCaptcha() throws Exception {
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
        Thread.sleep(2000);
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

    // 1) Solve method
    public void solveSmartCaptchaWithRuCaptcha() throws Exception {
        System.out.println("[INFO] Решаем Yandex SmartCaptcha (Selenium + 2Captcha)");

        String pageUrl = driver.getCurrentUrl();

        // 🔹 sitekey из div.smart-captcha
        WebElement captcha = driver.findElement(
                By.cssSelector("div.smart-captcha[data-sitekey]")
        );
        String siteKey = captcha.getAttribute("data-sitekey");

        System.out.println("[INFO] siteKey: " + siteKey);
        System.out.println("[INFO] pageUrl: " + pageUrl);

        // 🔹 createTask
        String taskId = sendRuCaptchaSmartTask(pageUrl, siteKey);
        System.out.println("[INFO] taskId: " + taskId);

        // 🔹 getTaskResult
        String token = getRuCaptchaSmartResult(taskId);
        System.out.println("[INFO] smart-token получен");

        // 🔹 вставка token
        ((JavascriptExecutor) driver).executeScript(
                "document.querySelector(\"input[data-testid='smart-token']\").value = arguments[0];",
                token
        );

        // 🔹 submit формы
        driver.findElement(
                By.cssSelector("form#check_humaneness button[type='submit']")
        ).click();

        Thread.sleep(2000);
        System.out.println("[SUCCESS] SmartCaptcha успешно решена");
    }

    // 2) Send task (createTask)
    private String sendRuCaptchaSmartTask(String pageUrl, String siteKey) throws Exception {
        URL url = new URL("https://api.2captcha.com/createTask");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.setDoOutput(true);

        String json = "{"
                + "\"clientKey\":\"" + rucaptchaKey + "\","
                + "\"task\":{"
                +   "\"type\":\"YandexSmartCaptchaTaskProxyless\","
                +   "\"websiteURL\":\"" + pageUrl + "\","
                +   "\"websiteKey\":\"" + siteKey + "\""
                + "}"
                + "}";

        con.getOutputStream().write(json.getBytes("UTF-8"));

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) sb.append(line);
        in.close();

        String response = sb.toString();
        System.out.println("[DEBUG] createTask response: " + response);

        if (!response.contains("\"taskId\"")) {
            throw new RuntimeException("2Captcha createTask error: " + response);
        }

        return response.replaceAll(".*\"taskId\"\\s*:\\s*(\\d+).*", "$1");
    }

    // 3) Get result (getTaskResult)
    private String getRuCaptchaSmartResult(String taskId) throws Exception {
        String json = "{"
                + "\"clientKey\":\"" + rucaptchaKey + "\","
                + "\"taskId\":" + taskId
                + "}";

        while (true) {
            Thread.sleep(5000);
            System.out.println("[INFO] getTaskResult... taskId=" + taskId);

            URL url = new URL("https://api.2captcha.com/getTaskResult");
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
            System.out.println("[DEBUG] getTaskResult response: " + response);

            if (response.contains("\"status\":\"ready\"")) {
                // ожидаемый формат: ..."solution":{"token":"..."}...
                if (!response.contains("\"token\"")) {
                    throw new RuntimeException("2Captcha ready, but no token: " + response);
                }
                return response.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
            }

            if (!response.contains("\"processing\"")) {
                throw new RuntimeException("2Captcha getTaskResult error: " + response);
            }
        }
    }

}