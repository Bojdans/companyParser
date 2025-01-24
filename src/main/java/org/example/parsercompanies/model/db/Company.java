package org.example.parsercompanies.model.db;

import lombok.Data;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Data
@Table(name = "companies") // Название таблицы в БД
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Первичный ключ

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
    private BigDecimal authorizedCapital; // Уставный капитал

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
    private BigDecimal revenue; // Выручка

    @Column(name = "profit")
    private BigDecimal profit; // Прибыль

    @Column(name = "capital")
    private BigDecimal capital; // Капитал

    @Column(name = "taxes")
    private BigDecimal taxes; // Налоги

    @Column(name = "insurance_contributions")
    private BigDecimal insuranceContributions; // Страховые взносы

    @Column(name = "government_purchases_customer")
    private String governmentPurchasesCustomer; // Госзакупки (Заказчик)

    @Column(name = "government_purchases_supplier")
    private String governmentPurchasesSupplier; // Госзакупки (Поставщик)

    @Column(name = "active_company")
    private Boolean activeCompany; // Действующая компания

    @Temporal(TemporalType.DATE)
    @Column(name = "registration_date")
    private Date registrationDate; // Дата регистрации

    @Column(name = "number_of_employees")
    private Integer numberOfEmployees; // Количество работников

    @Column(name = "okved_code")
    private String okvedCode; // Код ОКВЭД
}
