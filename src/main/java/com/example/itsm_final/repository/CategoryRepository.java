package com.example.itsm_final.repository;

import com.example.itsm_final.model.Category;
import com.example.itsm_final.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    List<Category> findByDepartmentOrderByIdAsc(Department department);

    List<Category> findByDepartmentIdOrderByIdAsc(Integer departmentId);

    Optional<Category> findByCategoryNameAndDepartment(String categoryName, Department department);
}
