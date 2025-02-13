package org.example.parsercompanies.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageViewController {
    @GetMapping("/page")
    public String getPage() {
        return "index.html"; 
    }
}