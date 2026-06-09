package com.example.itsm_final.service;

import com.example.itsm_final.model.Category;
import com.example.itsm_final.model.Department;
import com.example.itsm_final.model.Priority;
import com.example.itsm_final.model.Role;
import com.example.itsm_final.model.TicketStatus;
import com.example.itsm_final.model.User;
import com.example.itsm_final.repository.CategoryRepository;
import com.example.itsm_final.repository.DepartmentRepository;
import com.example.itsm_final.repository.PriorityRepository;
import com.example.itsm_final.repository.TicketStatusRepository;
import com.example.itsm_final.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Lookup tablolari icin tek capisma noktasi.
 * Controller'lar bu servisle konusur; repository'lere dogrudan dokunmazlar.
 */
@Service
@Transactional(readOnly = true)
public class LookupService {

    private final DepartmentRepository departmentRepo;
    private final CategoryRepository categoryRepo;
    private final PriorityRepository priorityRepo;
    private final TicketStatusRepository statusRepo;
    private final UserRepository userRepo;

    public LookupService(DepartmentRepository departmentRepo,
                         CategoryRepository categoryRepo,
                         PriorityRepository priorityRepo,
                         TicketStatusRepository statusRepo,
                         UserRepository userRepo) {
        this.departmentRepo = departmentRepo;
        this.categoryRepo = categoryRepo;
        this.priorityRepo = priorityRepo;
        this.statusRepo = statusRepo;
        this.userRepo = userRepo;
    }

    /** Atama dialog'u icin: hedef departmandaki ONAYLI Employee'lar. */
    public List<User> getAssignableEmployees(Integer departmentId) {
        return userRepo.findByDepartmentIdAndRoleIdAndApprovedTrue(
                departmentId, Role.EMPLOYEE_ID);
    }

    public List<Department> getAllDepartments() {
        return departmentRepo.findAll();
    }

    public Optional<Department> findDepartmentById(Integer id) {
        return departmentRepo.findById(id);
    }

    public List<Category> getCategoriesByDepartmentId(Integer departmentId) {
        return categoryRepo.findByDepartmentIdOrderByIdAsc(departmentId);
    }

    public Optional<Category> findCategoryById(Integer id) {
        return categoryRepo.findById(id);
    }

    public List<Priority> getAllPriorities() {
        return priorityRepo.findAllByOrderByLevelWeightAsc();
    }

    public Optional<Priority> findPriorityById(Integer id) {
        return priorityRepo.findById(id);
    }

    public List<TicketStatus> getAllStatuses() {
        return statusRepo.findAllByOrderByIdAsc();
    }

    public Optional<TicketStatus> findStatusById(Integer id) {
        return statusRepo.findById(id);
    }

    public TicketStatus getOpenStatus() {
        return statusRepo.findById(TicketStatus.OPEN_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Sistem hatası: 'Açık' durumu tabloda bulunamadı."));
    }
}
