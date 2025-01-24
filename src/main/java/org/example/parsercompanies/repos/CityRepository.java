package org.example.parsercompanies.repos;

import org.example.parsercompanies.model.db.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository  extends JpaRepository<City, Long> {
    List<City> findAllByisRegion(Boolean isRegion);
}
