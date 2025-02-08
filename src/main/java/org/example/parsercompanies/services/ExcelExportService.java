package org.example.parsercompanies.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.parsercompanies.model.db.Company;
import org.example.parsercompanies.repos.CompanyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ExcelExportService {

    private final CompanyRepository companyRepository;

    private boolean autoOpenExcel = true;

    @Value("${excel.export.directory:src/main/resources/excel}")
    private String exportDirectory;

    public ExcelExportService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public void exportToExcel() throws IOException {
        List<Company> companies = companyRepository.findAllByParsed(true);

        if (companies.isEmpty()) {
            System.out.println("No data to export.");
            return;
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Companies");

        
        Row headerRow = sheet.createRow(0);
        String[] headers = {"id","Рубрика", "Имя организации", "Руководитель", "Должность", "ИНН", "ОГРН", "ОКАТО",
                "Уставный капитал", "Юрид. адрес", "Город", "телефоны", "Email", "Сайт", "Выручка за год", "Прибыль за год",
                "Капитал", "Налоги", "Страховые взносы", "Госзакупки-заказчик",
                "Госзакупки-поставщик", "Компания действующая", "Дата регистрации", "Кол-во работников", "ОКВЭД"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderCellStyle(workbook));
        }

        
        int rowNum = 1;
        for (Company company : companies) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(1).setCellValue(company.getRubric() != null ? company.getRubric().toString() : "");
            row.createCell(0).setCellValue(company.getId());
            row.createCell(2).setCellValue(company.getOrganizationName());
            row.createCell(3).setCellValue(company.getFounder());
            row.createCell(4).setCellValue(company.getFounderPosition());
            row.createCell(5).setCellValue(company.getInn());
            row.createCell(6).setCellValue(company.getOgrn());
            row.createCell(7).setCellValue(company.getOkatoCode());
            row.createCell(8).setCellValue(company.getAuthorizedCapital() != null ? company.getAuthorizedCapital().toString() : "");
            row.createCell(9).setCellValue(company.getLegalAddress());
            row.createCell(10).setCellValue(company.getCity());
            row.createCell(11).setCellValue(company.getPhones());
            row.createCell(12).setCellValue(company.getEmail());
            row.createCell(13).setCellValue(company.getWebsite());
            row.createCell(14).setCellValue(company.getRevenue() != null ? company.getRevenue().toString() : "");
            row.createCell(15).setCellValue(company.getProfit() != null ? company.getProfit().toString() : "");
            row.createCell(16).setCellValue(company.getCapital() != null ? company.getCapital().toString() : "");
            row.createCell(17).setCellValue(company.getTaxes() != null ? company.getTaxes().toString() : "");
            row.createCell(18).setCellValue(company.getInsuranceContributions() != null ? company.getInsuranceContributions().toString() : "");
            row.createCell(19).setCellValue(company.getGovernmentPurchasesCustomer());
            row.createCell(20).setCellValue(company.getGovernmentPurchasesSupplier());
            row.createCell(21).setCellValue(company.getActiveCompany() != null && company.getActiveCompany() ? "Да" : "Нет");
            row.createCell(22).setCellValue(company.getRegistrationDate() != null ? company.getRegistrationDate().toString() : "");
            row.createCell(23).setCellValue(company.getNumberOfEmployees() != null ? company.getNumberOfEmployees().toString() : "");
            row.createCell(24).setCellValue(company.getOkvedCode());
        }

        
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        
        File directory = new File(exportDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        
        Path filePath = Paths.get(exportDirectory, "companies.xlsx");
        int counter = 1;
        while (filePath.toFile().exists()) {
            filePath = Paths.get(exportDirectory, "companies_" + counter + ".xlsx");
            counter++;
        }

        
        try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
            workbook.write(fileOut);
        }

        workbook.close();

        System.out.println("Data exported to: " + filePath);

        
        try {
            String command = "cmd /c start \"\" \"" + filePath+ "\"";
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}
