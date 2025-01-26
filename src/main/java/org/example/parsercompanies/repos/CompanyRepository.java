package org.example.parsercompanies.repos;

import jakarta.transaction.Transactional;
import org.example.parsercompanies.model.db.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM Category")
    void deleteAllEntities();
}
