package org.example.parsercompanies.model.db;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private int id;
    @Column(name = "category_name")
    private String name;
    @Column(name = "category_active")
    private boolean active = false;
    @Column(name = "category_level")
    private int level;
}
