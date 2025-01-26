package org.example.parsercompanies.model.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "categories")
@NoArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private long id;
    @Column(name = "category_name")
    private String name;
    @Column(name = "category_active")
    private boolean active = false;
    @Column(name = "category_level")
    private int level;

    public Category(String name, boolean active, int level) {
        this.name = name;
        this.active = active;
        this.level = level;
    }
}
