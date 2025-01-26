package org.example.parsercompanies.controllers;

import org.example.parsercompanies.model.db.Category;
import org.example.parsercompanies.model.db.City;
import org.example.parsercompanies.parsers.CategoriesParser;
import org.example.parsercompanies.repos.CategoryRepository;
import org.example.parsercompanies.repos.CityRepository;
import org.example.parsercompanies.repos.CompanyRepository;
import org.example.parsercompanies.services.ExcelExportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class APIController {
    private final CompanyRepository companyRepository;
    private CityRepository cityRepository;
    private CategoryRepository categoryRepository;
    private CategoriesParser categoriesParser;
    private ExcelExportService excelExportService;
    public APIController(CityRepository cityRepository, CategoryRepository categoryRepository, CategoriesParser categoriesParser, ExcelExportService excelExportService, CompanyRepository companyRepository) {
        this.cityRepository = cityRepository;
        this.categoryRepository = categoryRepository;
        this.categoriesParser = categoriesParser;
        this.excelExportService = excelExportService;
        this.companyRepository = companyRepository;
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
    @PostMapping("/startParsingCities")
    public void startParsing() {

    }
    @PostMapping("/stopParsingCities")
    public void stopParsing() {

    }
    @PostMapping("/exportCitiesDB")
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
//    @GetMapping("/isCompaniesParsed")
//    public boolean isCompaniesParsed(){
//        return com.isCategoriesParsed();
//    }
}
