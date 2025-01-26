package org.example.parsercompanies.model.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Table(name = "cities")
@Data
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "city_id")
    private Long id;

    @Column(name = "city_name")
    private String name;

    @Column(name = "city_is_region")
    private boolean isRegion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "parent_city_id")
    private List<City> daughters = new ArrayList<>();
    @Column(name = "city_url")
    private String url;

    public City(String name, boolean isRegion, List<City> daughters, String url) {
        this.name = name;
        this.isRegion = isRegion;
        this.daughters = daughters;
        this.url = url;
    }
}
