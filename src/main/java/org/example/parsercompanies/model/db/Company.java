package org.example.parsercompanies.model.db;

import lombok.Data;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "companies") // Название таблицы в БД
@NoArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Первичный ключ
    @Column
    private String url;
    @Column(name = "organization_type")
    private String organizationType;
    @Column(name = "organization_name")
    private String organizationName; // Название организации

    @Column(name = "founder")
    private String founder; // Учредитель

    @Column(name = "founder_position")
    private String founderPosition; // Должность учредителя

    @Column(name = "inn")
    private String inn; // ИНН

    @Column(name = "ogrn")
    private String ogrn; // ОГРН

    @Column(name = "okato_code")
    private String okatoCode; // Код ОКАТО

    @Column(name = "authorized_capital")
    private String authorizedCapital; // Уставный капитал

    @Column(name = "legal_address")
    private String legalAddress; // Юридический адрес

    @Column(name = "city")
    private String city; // Город

    @Column(name = "phones")
    private String phones; // Телефоны (можно хранить в формате JSON)

    @Column(name = "email")
    private String email; // Электронная почта

    @Column(name = "website")
    private String website; // Сайт

    @Column(name = "revenue")
    private String revenue; // Выручка

    @Column(name = "profit")
    private String profit; // Прибыль

    @Column(name = "capital")
    private String capital; // Капитал

    @Column(name = "taxes")
    private String taxes; // Налоги

    @Column(name = "insurance_contributions")
    private String insuranceContributions; // Страховые взносы

    @Column(name = "government_purchases_customer")
    private String governmentPurchasesCustomer; // Госзакупки (Заказчик)

    @Column(name = "government_purchases_supplier")
    private String governmentPurchasesSupplier; // Госзакупки (Поставщик)

    @Column(name = "active_company")
    private Boolean activeCompany; // Действующая компания

    @Column(name = "registration_date")
    private String registrationDate; // Дата регистрации

    @Column(name = "number_of_employees")
    private Integer numberOfEmployees; // Количество работников

    @Column(name = "okved_code")
    private String okvedCode; // Код ОКВЭД

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
