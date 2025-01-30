package org.example.parsercompanies.repos;

import jakarta.transaction.Transactional;
import org.example.parsercompanies.model.db.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM Company ")
    void deleteAllEntities();
    List<Company> findAllByParsed(Boolean parsed);
    Page<Company> findAllByParsed(Boolean parsed, Pageable pageable);
}
