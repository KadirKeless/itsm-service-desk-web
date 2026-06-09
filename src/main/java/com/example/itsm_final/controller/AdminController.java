package com.example.itsm_final.controller;

import com.example.itsm_final.dto.AdminCreateUserDto;
import com.example.itsm_final.dto.AdminUpdateUserDto;
import com.example.itsm_final.model.Role;
import com.example.itsm_final.repository.DepartmentRepository;
import com.example.itsm_final.repository.RoleRepository;
import com.example.itsm_final.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin paneli: kullanici yonetimi.
 * Tum endpoint'ler /admin altinda; SecurityConfig bu rotalari ADMIN rolune kisitlar.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;

    public AdminController(UserService userService,
                           RoleRepository roleRepository,
                           DepartmentRepository departmentRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
    }


    @GetMapping
    public String adminHome() {
        return "redirect:/admin/users";
    }

    @GetMapping("/users")
    public String users(@RequestParam(name = "filter", defaultValue = "pending") String filter,
                        Model model) {
        boolean onlyUnapproved = "pending".equalsIgnoreCase(filter);
        model.addAttribute("filter", onlyUnapproved ? "pending" : "all");

        var userList = onlyUnapproved
                ? userService.getUnapprovedUsers()
                : userService.getAllUsers();
        model.addAttribute("users", userList);

        // Sadece ATANABILECEK roller (Admin haric)
        List<Role> assignableRoles = roleRepository.findAll().stream()
                .filter(r -> !Role.ADMIN_ID.equals(r.getId()))
                .toList();
        model.addAttribute("roles", assignableRoles);
        model.addAttribute("departments", departmentRepository.findAll());

        if (!model.containsAttribute("createDto")) {
            model.addAttribute("createDto", new AdminCreateUserDto());
        }
        if (!model.containsAttribute("editDto")) {
            model.addAttribute("editDto", new AdminUpdateUserDto());
        }
        if (!model.containsAttribute("showCreateModal")) {
            model.addAttribute("showCreateModal", false);
        }
        if (!model.containsAttribute("showEditModal")) {
            model.addAttribute("showEditModal", false);
        }

        java.util.Map<Integer, Boolean> userAvatarMap = new java.util.HashMap<>();
        for (var u : userList) {
            userAvatarMap.put(u.getId(), userService.hasProfilePicture(u));
        }
        model.addAttribute("userAvatarMap", userAvatarMap);

        return "admin/users";
    }


    @PostMapping("/users/{id}/activate")
    public String activate(@PathVariable Integer id,
                           @RequestParam(value = "roleId", required = false) Integer roleId,
                           @RequestParam(value = "departmentId", required = false) Integer departmentId,
                           @RequestParam(value = "filter", defaultValue = "pending") String filter,
                           RedirectAttributes redirectAttrs) {
        try {
            userService.activateUser(id, roleId, departmentId);
            redirectAttrs.addFlashAttribute("successMessage", "Kullanıcı aktifleştirildi.");
        } catch (UserService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users?filter=" + filter;
    }

    @PostMapping("/users/{id}/freeze")
    public String freeze(@PathVariable Integer id,
                         @RequestParam(value = "filter", defaultValue = "all") String filter,
                         RedirectAttributes redirectAttrs) {
        try {
            userService.freezeUser(id);
            redirectAttrs.addFlashAttribute("successMessage", "Kullanıcı donduruldu.");
        } catch (UserService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users?filter=" + filter;
    }

    @PostMapping("/users/{id}/delete")
    public String delete(@PathVariable Integer id,
                         @RequestParam(value = "filter", defaultValue = "all") String filter,
                         RedirectAttributes redirectAttrs) {
        try {
            userService.deleteUser(id);
            redirectAttrs.addFlashAttribute("successMessage", "Kullanıcı silindi.");
        } catch (UserService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users?filter=" + filter;
    }


    @PostMapping("/users/bulk/activate")
    public String bulkActivate(@RequestParam("userIds") List<Integer> userIds,
                               @RequestParam(value = "roleId", required = false) Integer roleId,
                               @RequestParam(value = "departmentId", required = false) Integer departmentId,
                               @RequestParam(value = "filter", defaultValue = "pending") String filter,
                               RedirectAttributes redirectAttrs) {
        try {
            int count = userService.bulkActivateUsers(userIds, roleId, departmentId);
            redirectAttrs.addFlashAttribute("successMessage",
                    count + " kullanıcı aktifleştirildi.");
        } catch (UserService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users?filter=" + filter;
    }

    @PostMapping("/users/bulk/freeze")
    public String bulkFreeze(@RequestParam("userIds") List<Integer> userIds,
                             @RequestParam(value = "filter", defaultValue = "all") String filter,
                             RedirectAttributes redirectAttrs) {
        try {
            int count = userService.bulkFreezeUsers(userIds);
            redirectAttrs.addFlashAttribute("successMessage",
                    count + " kullanıcı donduruldu.");
        } catch (UserService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users?filter=" + filter;
    }

    @PostMapping("/users/bulk/delete")
    public String bulkDelete(@RequestParam("userIds") List<Integer> userIds,
                             @RequestParam(value = "filter", defaultValue = "all") String filter,
                             RedirectAttributes redirectAttrs) {
        try {
            int count = userService.bulkDeleteUsers(userIds);
            redirectAttrs.addFlashAttribute("successMessage",
                    count + " kullanıcı silindi.");
        } catch (UserService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users?filter=" + filter;
    }


    @PostMapping("/users/{id}/edit")
    public String submitEdit(@PathVariable Integer id,
                             @Valid @ModelAttribute("editDto") AdminUpdateUserDto dto,
                             BindingResult bindingResult,
                             @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
                             @RequestParam(value = "removeProfilePicture", defaultValue = "false") boolean removeProfilePicture,
                             @RequestParam(value = "filter", defaultValue = "all") String filter,
                             RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.editDto", bindingResult);
            redirectAttrs.addFlashAttribute("editDto", dto);
            redirectAttrs.addFlashAttribute("editUserId", id);
            redirectAttrs.addFlashAttribute("showEditModal", true);
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Form bilgilerinde hata var. Lütfen kontrol edip tekrar deneyiniz.");
            return "redirect:/admin/users?filter=" + filter;
        }
        try {
            if (removeProfilePicture) {
                userService.removeProfilePicture(id);
            } else if (profilePicture != null && !profilePicture.isEmpty()) {
                userService.updateProfilePicture(id, profilePicture);
            }
            userService.updateUser(id, dto);
            redirectAttrs.addFlashAttribute("successMessage", "Kullanıcı güncellendi.");
            return "redirect:/admin/users?filter=" + filter;
        } catch (UserService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("editDto", dto);
            redirectAttrs.addFlashAttribute("editUserId", id);
            redirectAttrs.addFlashAttribute("showEditModal", true);
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/users?filter=" + filter;
        }
    }


    @PostMapping("/users")
    public String create(@Valid @ModelAttribute("createDto") AdminCreateUserDto dto,
                         BindingResult bindingResult,
                         @RequestParam(value = "profilePicture", required = false) MultipartFile profilePicture,
                         @RequestParam(value = "filter", defaultValue = "all") String filter,
                         RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute(
                    "org.springframework.validation.BindingResult.createDto", bindingResult);
            dto.setPassword("");
            redirectAttrs.addFlashAttribute("createDto", dto);
            redirectAttrs.addFlashAttribute("showCreateModal", true);
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Form bilgilerinde hata var. Lütfen kontrol edip tekrar deneyiniz.");
            return "redirect:/admin/users?filter=" + filter;
        }
        try {
            var created = userService.adminCreateUser(dto);
            if (profilePicture != null && !profilePicture.isEmpty()) {
                userService.updateProfilePicture(created.getId(), profilePicture);
            }
            redirectAttrs.addFlashAttribute("successMessage", "Yeni kullanıcı oluşturuldu.");
        } catch (UserService.EmailAlreadyExistsException | UserService.BusinessException ex) {
            dto.setPassword("");
            redirectAttrs.addFlashAttribute("createDto", dto);
            redirectAttrs.addFlashAttribute("showCreateModal", true);
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users?filter=" + filter;
    }
}
