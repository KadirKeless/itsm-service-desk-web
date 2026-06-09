package com.example.itsm_final.controller;

import com.example.itsm_final.service.LookupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Frontend'in AJAX cagrilari icin lookup endpoint'leri.
 * Yeni talep formunda departman secilince ona ait kategoriler bu endpoint'ten
 * cekilir.
 */
@RestController
@RequestMapping("/api/lookup")
public class LookupApiController {

    private final LookupService lookupService;

    public LookupApiController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    /** Bir departmana ait kategorileri dondurur (id + ad). */
    @GetMapping("/departments/{departmentId}/categories")
    public List<CategoryView> categoriesByDepartment(@PathVariable Integer departmentId) {
        return lookupService.getCategoriesByDepartmentId(departmentId).stream()
                .map(c -> new CategoryView(c.getId(), c.getCategoryName()))
                .toList();
    }

    public record CategoryView(Integer id, String name) {}
}
