package org.example.parsercompanies.controllers;

import org.example.parsercompanies.model.db.Category;
import org.example.parsercompanies.model.db.City;
import org.example.parsercompanies.model.db.Company;
import org.example.parsercompanies.parsers.CategoriesParser;
import org.example.parsercompanies.parsers.CompanyParser;
import org.example.parsercompanies.repos.CategoryRepository;
import org.example.parsercompanies.repos.CityRepository;
import org.example.parsercompanies.repos.CompanyRepository;
import org.example.parsercompanies.services.ExcelExportService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;

@RestController
@RequestMapping("/api")
public class APIController {
    private final CompanyRepository companyRepository;
    private CityRepository cityRepository;
    private CategoryRepository categoryRepository;
    private CategoriesParser categoriesParser;
    private ExcelExportService excelExportService;
    private CompanyParser companyParser;
    private final ApplicationContext context;

    public APIController(CityRepository cityRepository, CategoryRepository categoryRepository, CategoriesParser categoriesParser, ExcelExportService excelExportService, CompanyRepository companyRepository, CompanyParser companyParser, ApplicationContext context) {
        this.cityRepository = cityRepository;
        this.categoryRepository = categoryRepository;
        this.categoriesParser = categoriesParser;
        this.excelExportService = excelExportService;
        this.companyRepository = companyRepository;
        this.companyParser = companyParser;
        this.context = context;

    }
    @GetMapping("/getCitiesAndRegions")
    public List<City> getAllCitiesAndRegions() {
        return cityRepository.findAll();
    }
    @GetMapping("/getRegions")
    public List<City> getRegions() {
        return cityRepository.findAllByisRegion(true);
    }
    @PostMapping("/updateCategories")
    public ResponseEntity<String> updateRubrics(@RequestBody List<Category> categories) {
        try {
            categoryRepository.saveAll(categories);
            return ResponseEntity.ok("Rubrics updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update rubrics.");
        }
    }

    @GetMapping("/getCategories")
    public ResponseEntity<List<Category>> getCategories() {
        List<Category> rubrics = categoryRepository.findAll();
        return ResponseEntity.ok(rubrics);
    }
    @PostMapping("/startParsingCompanies")
    public void startParsing() throws IOException, InterruptedException {
        companyParser.startParsing();
    }
    @PostMapping("/stopParsingCompanies")
    public void stopParsing() {
        companyParser.stopParsing();
    }
    @Async
    @PostMapping("/shutdown")
    public void shutdown() throws IOException {
        companyParser.stopParsing();
        
        System.out.println("Closing application...");
        
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

        
        System.out.println("Terminating process with PID: " + pid);
        Runtime.getRuntime().exec("taskkill /F /PID " + pid); 
        Runtime.getRuntime().exec("kill -9 " + pid); 

        System.exit(0);
        
        String command = "taskkill /F /IM java.exe";
        
        Process process = Runtime.getRuntime().exec(command);
        SpringApplication.exit(context, () -> 0);
        System.exit(0);
    }
    @PostMapping("/exportCompaniesDB")
    public void ExportDB() throws IOException {
        excelExportService.exportToExcel();
    }
    @PostMapping("/cleanCompanies")
    public void cleanCompanies() {
        companyRepository.deleteAll();
    }
    @PostMapping("/startParsingCategories")
    public void startParsingCategories() throws IOException, InterruptedException {
        categoriesParser.startParsing();
    }
    @GetMapping("/isCategoriesParsed")
    public boolean isCategoriesParsed(){
        return categoriesParser.isCategoriesParsed();
    }

    @GetMapping("/searchCategories")
    public List<Category> searchCategories(@RequestParam String query) {
        return categoryRepository.searchByName(query);
    }
    @GetMapping("/getActiveCategories")
    public  List<Category> findAllByActive(){
        return categoryRepository.findAllByActive(true);
    }
    @GetMapping("/getAllCompanies")
    public Page<Company> getAllCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("id").ascending());
        return companyRepository.findAllByParsed(true, pageable);
    }
    @GetMapping("/parsingStatus")
    public boolean parsingStatus(){
        return companyParser.isCompaniesParsed();
    }
    @GetMapping("/getLogStatus")
    public String getLogStatus(){
        return CompanyParser.logStatus;
    }
}
