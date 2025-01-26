package org.example.parsercompanies.repos;

import jakarta.transaction.Transactional;
import org.example.parsercompanies.model.db.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByActive(Boolean active);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<Category> searchByName(@Param("searchText") String searchText);

    @Modifying
    @Transactional
    @Query("DELETE FROM Category")
    void deleteAllEntities();
}
