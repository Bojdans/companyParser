package org.example.parsercompanies.controllers;

import org.example.parsercompanies.model.db.Category;
import org.example.parsercompanies.model.db.City;
import org.example.parsercompanies.repos.CategoryRepository;
import org.example.parsercompanies.repos.CityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class APIController {
    private CityRepository cityRepository;
    private CategoryRepository categoryRepository;
    public APIController(CityRepository cityRepository, CategoryRepository categoryRepository) {
        this.cityRepository = cityRepository;
        this.categoryRepository = categoryRepository;
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
    @PostMapping("/startParsing")
    public void startParsing() {

    }
    @PostMapping("/stopParsing")
    public void stopParsing() {

    }
    @PostMapping("/exportDB")
    public void ExportDB(){

    }
    @PostMapping("/cleanCompanies")
    public void cleanCompanies() {

    }
}
