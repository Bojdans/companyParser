package org.example.parsercompanies.model.db;

import lombok.Data;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "companies") 
@NoArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 
    @Column
    private String url;
    @Column(name = "rubric")
    private String rubric;
    @Column(name = "organization_type")
    private String organizationType;
    @Column(name = "organization_name")
    private String organizationName; 

    @Column(name = "founder")
    private String founder; 

    @Column(name = "founder_position")
    private String founderPosition; 

    @Column(name = "inn")
    private String inn; 

    @Column(name = "ogrn")
    private String ogrn; 

    @Column(name = "okato_code")
    private String okatoCode; 

    @Column(name = "authorized_capital")
    private String authorizedCapital = "0"; 

    @Column(name = "legal_address")
    private String legalAddress; 

    @Column(name = "city")
    private String city; 

    @Column(name = "phones")
    private String phones; 

    @Column(name = "email")
    private String email; 

    @Column(name = "website")
    private String website; 

    @Column(name = "revenue")
    private String revenue = "0 руб"; 

    @Column(name = "profit")
    private String profit = "0 руб"; 

    @Column(name = "capital")
    private String capital = "0 руб"; 

    @Column(name = "taxes")
    private String taxes = "0 руб"; 

    @Column(name = "insurance_contributions")
    private String insuranceContributions = "0 руб"; 

    @Column(name = "government_purchases_customer")
    private String governmentPurchasesCustomer = "0 руб"; 

    @Column(name = "government_purchases_supplier")
    private String governmentPurchasesSupplier = "0 руб"; 

    @Column(name = "active_company")
    private Boolean activeCompany = true; 

    @Column(name = "registration_date")
    private String registrationDate; 

    @Column(name = "number_of_employees")
    private Integer numberOfEmployees = 0; 

    @Column(name = "okved_code")
    private String okvedCode; 

    @Column(name = "parsed")
    private boolean parsed = false;

    public Company(String organizationName, String founder, String founderPosition, String inn, String ogrn, String okatoCode, String authorizedCapital, String legalAddress, String city, String phones, String email, String website, String revenue, String profit, String capital, String taxes, String insuranceContributions, String governmentPurchasesCustomer, String governmentPurchasesSupplier, Boolean activeCompany, String registrationDate, Integer numberOfEmployees, String okvedCode) {
        this.organizationName = organizationName;
        this.founder = founder;
        this.founderPosition = founderPosition;
        this.inn = inn;
        this.ogrn = ogrn;
        this.okatoCode = okatoCode;
        this.authorizedCapital = authorizedCapital;
        this.legalAddress = legalAddress;
        this.city = city;
        this.phones = phones;
        this.email = email;
        this.website = website;
        this.revenue = revenue;
        this.profit = profit;
        this.capital = capital;
        this.taxes = taxes;
        this.insuranceContributions = insuranceContributions;
        this.governmentPurchasesCustomer = governmentPurchasesCustomer;
        this.governmentPurchasesSupplier = governmentPurchasesSupplier;
        this.activeCompany = activeCompany;
        this.registrationDate = registrationDate;
        this.numberOfEmployees = numberOfEmployees;
        this.okvedCode = okvedCode;
    }
    public Company(Long id,String organizationName, String founder, String founderPosition, String inn, String ogrn, String okatoCode, String authorizedCapital, String legalAddress, String city, String phones, String email, String website, String revenue, String profit, String capital, String taxes, String insuranceContributions, String governmentPurchasesCustomer, String governmentPurchasesSupplier, Boolean activeCompany, String registrationDate, Integer numberOfEmployees, String okvedCode) {
        this.organizationName = organizationName;
        this.founder = founder;
        this.founderPosition = founderPosition;
        this.inn = inn;
        this.ogrn = ogrn;
        this.okatoCode = okatoCode;
        this.authorizedCapital = authorizedCapital;
        this.legalAddress = legalAddress;
        this.city = city;
        this.phones = phones;
        this.email = email;
        this.website = website;
        this.revenue = revenue;
        this.profit = profit;
        this.capital = capital;
        this.taxes = taxes;
        this.insuranceContributions = insuranceContributions;
        this.governmentPurchasesCustomer = governmentPurchasesCustomer;
        this.governmentPurchasesSupplier = governmentPurchasesSupplier;
        this.activeCompany = activeCompany;
        this.registrationDate = registrationDate;
        this.numberOfEmployees = numberOfEmployees;
        this.okvedCode = okvedCode;
        this.id = id;
    }

    public Company(String url) {
        this.url = url;
    }
}
