package com.example.itsm_final.controller;

import com.example.itsm_final.dto.CreateTicketDto;
import com.example.itsm_final.dto.ReplyDto;
import com.example.itsm_final.model.Department;
import com.example.itsm_final.model.Role;
import com.example.itsm_final.model.Ticket;
import com.example.itsm_final.model.TicketStatus;
import com.example.itsm_final.model.User;
import com.example.itsm_final.security.CustomUserDetails;
import com.example.itsm_final.service.LookupService;
import com.example.itsm_final.service.TicketReplyService;
import com.example.itsm_final.service.TicketService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/tickets")
public class TicketController {

    private static final Logger log = LoggerFactory.getLogger(TicketController.class);

    private final TicketService ticketService;
    private final TicketReplyService replyService;
    private final LookupService lookupService;

    public TicketController(TicketService ticketService,
                            TicketReplyService replyService,
                            LookupService lookupService) {
        this.ticketService = ticketService;
        this.replyService = replyService;
        this.lookupService = lookupService;
    }


    @GetMapping("/new")
    public String newTicketForm(@AuthenticationPrincipal CustomUserDetails principal,
                                Model model) {
        if (!model.containsAttribute("createTicketDto")) {
            model.addAttribute("createTicketDto", new CreateTicketDto());
        }
        addFormReferenceData(model, principal.getUser());
        return "tickets/new";
    }

    @PostMapping("/new")
    public String createTicket(@Valid @ModelAttribute("createTicketDto") CreateTicketDto dto,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               Model model,
                               RedirectAttributes redirectAttrs) {

        User requester = principal.getUser();

        if (bindingResult.hasErrors()) {
            addFormReferenceData(model, requester);
            return "tickets/new";
        }

        try {
            ticketService.createTicket(dto, requester);
        } catch (TicketService.BusinessException ex) {
            bindingResult.reject("ticket.create.error", ex.getMessage());
            addFormReferenceData(model, requester);
            return "tickets/new";
        }

        redirectAttrs.addFlashAttribute("successMessage",
                "Talebiniz başarıyla oluşturuldu. İlgili departman size dönüş yapacaktır.");
        return "redirect:/tickets/my";
    }


    @GetMapping("/my")
    public String myTickets(@AuthenticationPrincipal CustomUserDetails principal, Model model) {
        User user = principal.getUser();
        model.addAttribute("tickets", ticketService.findMyTickets(user));
        model.addAttribute("pageTitle", "Taleplerim");
        model.addAttribute("emptyMessage", "Henüz bir talep oluşturmadınız.");
        model.addAttribute("listSource", "my");
        model.addAttribute("navActive", "ticket-my");
        model.addAttribute("showCancelActions", true);
        model.addAttribute("currentUserId", user.getId());
        return "tickets/list";
    }

    @GetMapping("/all")
    public String allTickets(Model model) {
        model.addAttribute("tickets", ticketService.findAll());
        model.addAttribute("pageTitle", "Talep Yönetimi");
        model.addAttribute("emptyMessage", "Sistemde henüz hiç talep yok.");
        model.addAttribute("listSource", "all");
        model.addAttribute("navActive", "ticket-all");
        return "tickets/list";
    }

    @GetMapping("/department")
    public String departmentTickets(@AuthenticationPrincipal CustomUserDetails principal,
                                    Model model) {
        User user = principal.getUser();
        if (user.getDepartment() == null) {
            model.addAttribute("tickets", List.of());
            model.addAttribute("pageTitle", "Departman Talepleri");
            model.addAttribute("emptyMessage", "Departmanınız atanmamış.");
            model.addAttribute("listSource", "department");
            model.addAttribute("navActive", "ticket-dept");
            return "tickets/list";
        }
        model.addAttribute("tickets",
                ticketService.findByDepartmentId(user.getDepartment().getId()));
        model.addAttribute("pageTitle", "Departman Talepleri");
        model.addAttribute("emptyMessage", "Departmanınıza yönelmiş talep yok.");
        model.addAttribute("listSource", "department");
        model.addAttribute("navActive", "ticket-dept");
        return "tickets/list";
    }

    @GetMapping("/assigned")
    public String assignedTickets(@AuthenticationPrincipal CustomUserDetails principal,
                                  Model model) {
        model.addAttribute("tickets", ticketService.findAssignedTo(principal.getUser()));
        model.addAttribute("pageTitle", "Bana Atanan Talepler");
        model.addAttribute("emptyMessage", "Üzerinizde aktif bir talep yok.");
        model.addAttribute("listSource", "assigned");
        model.addAttribute("navActive", "ticket-assigned");
        return "tickets/list";
    }


    @GetMapping("/{id}")
    public String ticketDetail(@PathVariable Integer id,
                               @RequestParam(value = "from", required = false) String from,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               Model model) {
        User user = principal.getUser();
        Ticket ticket = ticketService.findById(id)
                .orElseThrow(() -> new TicketService.BusinessException("Talep bulunamadı: #" + id));

        if (!ticketService.canView(ticket, user)) {
            throw new AccessDeniedException("Bu talebi görüntüleme yetkiniz yok.");
        }

        boolean isClosed = isClosed(ticket);
        boolean isAdmin = isAdmin(user);
        boolean canAssign = ticketService.canAssign(ticket, user);
        boolean canChangeStatus = ticketService.canChangeStatus(ticket, user);
        boolean isAssignedEmployee = isAssignedEmployee(ticket, user);
        boolean canDelete = ticketService.canDelete(user);

        // Atanmis calisan icin gosterilecek olan durumlar (sadece 3 ve 4)
        List<TicketStatus> statusOptions;
        if (isAssignedEmployee && !isAdmin && !canAssign) {
            statusOptions = new ArrayList<>();
            for (TicketStatus status : lookupService.getAllStatuses()) {
                Integer statusId = status.getId();
                if (TicketStatus.RESOLVED_ID.equals(statusId)
                        || TicketStatus.CANCELLED_ID.equals(statusId)) {
                    statusOptions.add(status);
                }
            }
        } else {
            statusOptions = lookupService.getAllStatuses();
        }

        model.addAttribute("ticket", ticket);
        model.addAttribute("replies", replyService.getRepliesForTicket(ticket));
        model.addAttribute("currentUser", user);
        model.addAttribute("isClosed", isClosed);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("canAssign", canAssign);
        model.addAttribute("canChangeStatus", canChangeStatus);
        model.addAttribute("canDelete", canDelete);
        model.addAttribute("statusOptions", statusOptions);
        if (canAssign) {
            model.addAttribute("assignableEmployees",
                    lookupService.getAssignableEmployees(ticket.getTargetDepartment().getId()));
        }
        if (!model.containsAttribute("replyDto")) {
            model.addAttribute("replyDto", new ReplyDto());
        }
        addBackLinkInfo(model, user, ticket, from);
        String listSource = resolveListSource(from, user, ticket);
        model.addAttribute("navActive", navActiveForListSource(listSource));
        return "tickets/detail";
    }


    @PostMapping("/{id}/reply")
    public String addReply(@PathVariable Integer id,
                           @Valid @ModelAttribute("replyDto") ReplyDto dto,
                           BindingResult bindingResult,
                           @AuthenticationPrincipal CustomUserDetails principal,
                           RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError("message");
            String errorMessage = "Geçersiz yanıt.";
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                errorMessage = fieldError.getDefaultMessage();
            }
            redirectAttrs.addFlashAttribute("errorMessage", errorMessage);
            return "redirect:/tickets/" + id;
        }
        try {
            replyService.addReply(id, dto.getMessage(), principal.getUser());
            redirectAttrs.addFlashAttribute("successMessage", "Yanıtınız eklendi.");
        } catch (TicketReplyService.ReplyException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("[POST /tickets/{}/reply] Beklenmedik hata", id, ex);
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Yanıt eklenirken beklenmedik bir hata oluştu: " + ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/{id}/reply/{replyId}/edit")
    public String editReply(@PathVariable Integer id,
                            @PathVariable Integer replyId,
                            @RequestParam("message") String message,
                            @AuthenticationPrincipal CustomUserDetails principal,
                            RedirectAttributes redirectAttrs) {
        try {
            replyService.updateReply(replyId, message, principal.getUser(),
                    isAdmin(principal.getUser()));
            redirectAttrs.addFlashAttribute("successMessage", "Yanıt güncellendi.");
        } catch (TicketReplyService.ReplyException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("[POST /tickets/{}/reply/{}/edit] Beklenmedik hata", id, replyId, ex);
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Yanıt güncellenirken beklenmedik bir hata oluştu: " + ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/{id}/reply/{replyId}/delete")
    public String deleteReply(@PathVariable Integer id,
                              @PathVariable Integer replyId,
                              @AuthenticationPrincipal CustomUserDetails principal,
                              RedirectAttributes redirectAttrs) {
        try {
            replyService.deleteReply(replyId, principal.getUser(),
                    isAdmin(principal.getUser()));
            redirectAttrs.addFlashAttribute("successMessage", "Yanıt silindi.");
        } catch (TicketReplyService.ReplyException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("[POST /tickets/{}/reply/{}/delete] Beklenmedik hata", id, replyId, ex);
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Yanıt silinirken beklenmedik bir hata oluştu: " + ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }


    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Integer id,
                               @RequestParam("statusId") Integer statusId,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttrs) {
        User user = principal.getUser();
        try {
            Ticket ticket = ticketService.findById(id)
                    .orElseThrow(() -> new TicketService.BusinessException("Talep bulunamadı."));

            if (!ticketService.canChangeStatus(ticket, user)) {
                throw new TicketService.BusinessException("Bu talebin durumunu güncelleme yetkiniz yok.");
            }
            // Atanmis calisan sadece 3/4'e gecirebilir
            if (isAssignedEmployee(ticket, user) && !isAdmin(user)
                    && !ticketService.canAssign(ticket, user)) {
                if (!statusId.equals(TicketStatus.RESOLVED_ID)
                        && !statusId.equals(TicketStatus.CANCELLED_ID)) {
                    throw new TicketService.BusinessException(
                            "Çalışan yalnızca 'Çözüldü' veya 'İptal Edildi' durumunu seçebilir.");
                }
            }

            ticketService.updateStatus(id, statusId);
            redirectAttrs.addFlashAttribute("successMessage", "Talep durumu güncellendi.");
        } catch (TicketService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("[POST /tickets/{}/status] Beklenmedik hata", id, ex);
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Durum güncellenirken beklenmedik bir hata oluştu: " + ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/{id}/assign")
    public String assignTicket(@PathVariable Integer id,
                               @RequestParam(value = "assigneeId", required = false) Integer assigneeId,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttrs) {
        User user = principal.getUser();
        try {
            if (assigneeId == null) {
                throw new TicketService.BusinessException("Lütfen atanacak bir çalışan seçiniz.");
            }
            Ticket ticket = ticketService.findById(id)
                    .orElseThrow(() -> new TicketService.BusinessException("Talep bulunamadı."));
            if (!ticketService.canAssign(ticket, user)) {
                throw new TicketService.BusinessException("Bu talebi atama yetkiniz yok.");
            }
            ticketService.assignTicket(id, assigneeId);
            redirectAttrs.addFlashAttribute("successMessage", "Talep atandı.");
        } catch (TicketService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("[POST /tickets/{}/assign] Beklenmedik hata (assigneeId={})", id, assigneeId, ex);
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Atama yapılırken beklenmedik bir hata oluştu: " + ex.getMessage());
        }
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelTicket(@PathVariable Integer id,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttrs) {
        try {
            ticketService.cancelByRequester(id, principal.getUser());
            redirectAttrs.addFlashAttribute("successMessage", "Talebiniz iptal edildi.");
        } catch (TicketService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            log.error("[POST /tickets/{}/cancel] Beklenmedik hata", id, ex);
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Talep iptal edilirken beklenmedik bir hata oluştu: " + ex.getMessage());
        }
        return "redirect:/tickets/my";
    }

    @PostMapping("/{id}/delete")
    public String deleteTicket(@PathVariable Integer id,
                               @AuthenticationPrincipal CustomUserDetails principal,
                               RedirectAttributes redirectAttrs) {
        try {
            if (!ticketService.canDelete(principal.getUser())) {
                throw new TicketService.BusinessException("Talep silmeye yalnızca admin yetkilidir.");
            }
            ticketService.deleteTicket(id);
            redirectAttrs.addFlashAttribute("successMessage", "Talep silindi.");
            return "redirect:/tickets/all";
        } catch (TicketService.BusinessException ex) {
            redirectAttrs.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/tickets/" + id;
        } catch (Exception ex) {
            log.error("[POST /tickets/{}/delete] Beklenmedik hata", id, ex);
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Talep silinirken beklenmedik bir hata oluştu: " + ex.getMessage());
            return "redirect:/tickets/" + id;
        }
    }


    private void addFormReferenceData(Model model, User user) {
        List<Department> departments = new ArrayList<>();
        for (Department department : lookupService.getAllDepartments()) {
            if (user.getDepartment() == null
                    || !department.getId().equals(user.getDepartment().getId())) {
                departments.add(department);
            }
        }
        model.addAttribute("departments", departments);
        model.addAttribute("priorities", lookupService.getAllPriorities());
    }

    private boolean isAdmin(User user) {
        return user.getRole() != null && Role.ADMIN_ID.equals(user.getRole().getId());
    }

    private boolean isAssignedEmployee(Ticket ticket, User user) {
        return ticket.getAssignedUser() != null
                && ticket.getAssignedUser().getId().equals(user.getId());
    }

    private boolean isClosed(Ticket ticket) {
        Integer sid = ticket.getStatus() != null ? ticket.getStatus().getId() : null;
        return sid != null
                && (sid.equals(TicketStatus.RESOLVED_ID) || sid.equals(TicketStatus.CANCELLED_ID));
    }

    /**
     * Detay sayfasindaki "Listeye don" linkini, kullanicinin geldigi listeye gore belirler.
     * ?from=assigned|my|department|all parametresi varsa onu kullanir;
     * yoksa kullanicinin taleple iliskisine gore tahmin eder.
     */
    private void addBackLinkInfo(Model model, User user, Ticket ticket, String from) {
        String source = resolveListSource(from, user, ticket);
        if ("assigned".equals(source)) {
            model.addAttribute("backUrl", "/tickets/assigned");
            model.addAttribute("backLabel", "Bana Atanan Talepler");
        } else if ("department".equals(source)) {
            model.addAttribute("backUrl", "/tickets/department");
            model.addAttribute("backLabel", "Departman Talepleri");
        } else if ("all".equals(source)) {
            model.addAttribute("backUrl", "/tickets/all");
            model.addAttribute("backLabel", "Talep Yönetimi");
        } else {
            model.addAttribute("backUrl", "/tickets/my");
            model.addAttribute("backLabel", "Taleplerim");
        }
    }

    private String resolveListSource(String from, User user, Ticket ticket) {
        if (from != null && !from.isBlank()) {
            return from.trim();
        }
        return inferListSource(user, ticket);
    }

    private String inferListSource(User user, Ticket ticket) {
        if (ticket.getRequester() != null && ticket.getRequester().getId().equals(user.getId())) {
            return "my";
        }
        if (isAssignedEmployee(ticket, user)) {
            return "assigned";
        }
        if (isAdmin(user)) {
            return "all";
        }
        if (user.getRole() != null && Role.MANAGER_ID.equals(user.getRole().getId())
                && user.getDepartment() != null
                && ticket.getTargetDepartment() != null
                && user.getDepartment().getId().equals(ticket.getTargetDepartment().getId())) {
            return "department";
        }
        return "my";
    }

    private String navActiveForListSource(String source) {
        if ("assigned".equals(source)) {
            return "ticket-assigned";
        }
        if ("department".equals(source)) {
            return "ticket-dept";
        }
        if ("all".equals(source)) {
            return "ticket-all";
        }
        if ("my".equals(source)) {
            return "ticket-my";
        }
        return "";
    }
}
