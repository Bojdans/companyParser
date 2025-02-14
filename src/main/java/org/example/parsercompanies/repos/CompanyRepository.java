package org.example.parsercompanies.repos;
import org.example.parsercompanies.model.db.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findAllByParsed(Boolean parsed);
    Page<Company> findAllByParsed(Boolean parsed, Pageable pageable);
}
