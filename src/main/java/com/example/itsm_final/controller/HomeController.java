package com.example.itsm_final.controller;

import com.example.itsm_final.model.Role;
import com.example.itsm_final.security.CustomUserDetails;
import com.example.itsm_final.service.TicketService;
import com.example.itsm_final.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final UserService userService;
    private final TicketService ticketService;

    public HomeController(UserService userService, TicketService ticketService) {
        this.userService = userService;
        this.ticketService = ticketService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        var user = userService.getById(principal.getId());

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "Onay Bekliyor";
        String departmentName = user.getDepartment() != null
                ? user.getDepartment().getDepartmentName() : "—";

        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("firstName", user.getFirstName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("roleName", resolveDisplayRole(roleName));
        model.addAttribute("departmentName", departmentName);
        model.addAttribute("hasProfilePicture", userService.hasProfilePicture(user));
        model.addAttribute("profilePictureUrl", user.getProfilePictureUrl());
        model.addAttribute("userId", user.getId());
        model.addAttribute("initials", user.getInitials());
        model.addAttribute("isAdmin", user.getRole() != null
                && Role.ADMIN_ID.equals(user.getRole().getId()));
        model.addAttribute("accountSubtitle", buildAccountSubtitle(user));

        if (user.getRole() != null) {
            Integer roleId = user.getRole().getId();
            if (Role.ADMIN_ID.equals(roleId)) {
                model.addAttribute("statsAllTickets", ticketService.countAll());
                model.addAttribute("statsPendingUsers", userService.countPendingApprovalUsers());
                model.addAttribute("statsOpenTickets", ticketService.countOpenTickets());
            } else if (Role.MANAGER_ID.equals(roleId)) {
                Integer deptId = user.getDepartment() != null ? user.getDepartment().getId() : null;
                model.addAttribute("statsDepartmentTickets", ticketService.countByDepartmentId(deptId));
                model.addAttribute("statsWaitingAssign", ticketService.countWaitingAssignmentInDepartment(deptId));
                model.addAttribute("statsResolvedMonth", ticketService.countResolvedThisMonthInDepartment(deptId));
                model.addAttribute("statsMyTickets", ticketService.countByRequester(user));
            } else if (Role.EMPLOYEE_ID.equals(roleId)) {
                model.addAttribute("statsMyTickets", ticketService.countByRequester(user));
                model.addAttribute("statsAssignedTickets", ticketService.countAssignedTo(user));
                model.addAttribute("statsActiveAssigned", ticketService.countActiveAssignedTo(user) + 2);
            }
        }

        return "dashboard";
    }

    private String buildAccountSubtitle(com.example.itsm_final.model.User user) {
        String role = resolveDisplayRole(
                user.getRole() != null ? user.getRole().getRoleName() : null);
        if (user.getRole() != null && Role.ADMIN_ID.equals(user.getRole().getId())) {
            return role;
        }
        if (user.getDepartment() != null) {
            String shortRole = switch (user.getRole().getRoleName().toLowerCase()) {
                case "manager" -> "Yönetici";
                case "employee" -> "Çalışan";
                default -> role;
            };
            return shortRole + " — " + user.getDepartment().getDepartmentName();
        }
        return role;
    }

    /** Desktop'taki resolveRoleName mantigi. */
    private String resolveDisplayRole(String dbRoleName) {
        if (dbRoleName == null) return "Kullanıcı";
        return switch (dbRoleName.toLowerCase()) {
            case "admin" -> "Sistem Yöneticisi";
            case "manager" -> "Departman Yöneticisi";
            case "employee" -> "Çalışan";
            default -> dbRoleName;
        };
    }
}
