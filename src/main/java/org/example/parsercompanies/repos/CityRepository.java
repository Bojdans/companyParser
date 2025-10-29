package org.example.parsercompanies.repos;

import jakarta.transaction.Transactional;
import org.example.parsercompanies.model.db.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository  extends JpaRepository<City, Long> {
    List<City> findAllByisRegion(Boolean isRegion);
}