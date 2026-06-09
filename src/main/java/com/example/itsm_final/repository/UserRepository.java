package com.example.itsm_final.repository;

import com.example.itsm_final.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Departmanin uyeleri (atama dialog'u icin: rol Employee + approved).
    List<User> findByDepartmentIdAndRoleIdAndApprovedTrue(Integer departmentId, Integer roleId);

    // Admin panelinde "Onay Bekleyenler": is_approved=0, rol ve departman bos.
    List<User> findByApprovedFalseAndRoleIsNullAndDepartmentIsNullOrderByIdAsc();

    long countByApprovedFalseAndRoleIsNullAndDepartmentIsNull();

    List<User> findAllByOrderByIdAsc();

    //belirli departmanda belirli rolden kac onayli kullanici var.
    long countByDepartmentIdAndRoleIdAndApprovedTrue(Integer departmentId, Integer roleId);
}
