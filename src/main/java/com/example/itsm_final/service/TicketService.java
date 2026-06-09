package com.example.itsm_final.service;

import com.example.itsm_final.dto.CreateTicketDto;
import com.example.itsm_final.model.Category;
import com.example.itsm_final.model.Department;
import com.example.itsm_final.model.Priority;
import com.example.itsm_final.model.Role;
import com.example.itsm_final.model.Ticket;
import com.example.itsm_final.model.TicketStatus;
import com.example.itsm_final.model.User;
import com.example.itsm_final.repository.TicketRepository;
import com.example.itsm_final.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Talep is mantiginin tek sahibi. Controller'lar yalnizca bu servisle konusur.
 *
 * Desktop davranisini birebir yansitir:
 *  - Kullanici kendi departmanina talep acamaz
 *  - Yeni talep her zaman durum=OPEN (1) ile baslar
 *  - Status 3/4 (Cozuldu/Iptal) kapali sayilir, geri donus yok
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepo;
    private final LookupService lookupService;
    private final UserRepository userRepo;

    public TicketService(TicketRepository ticketRepo,
                         LookupService lookupService,
                         UserRepository userRepo) {
        this.ticketRepo = ticketRepo;
        this.lookupService = lookupService;
        this.userRepo = userRepo;
    }

    /**
     * Yeni talep olusturur. Iliskili lookup entity'lerini DB'den dogrular;
     * bulunamazsa veya is kurali ihlal edilirse domain exception firlatir.
     */
    @Transactional
    public Ticket createTicket(CreateTicketDto dto, User requester) {

        // 1) Hedef departman gecerli mi
        Department targetDept = lookupService.findDepartmentById(dto.getTargetDepartmentId())
                .orElseThrow(() -> new BusinessException(
                        "Seçilen departman bulunamadı."));

        // 2) Kendi departmanına talep acamazsiniz
        if (requester.getDepartment() != null
                && requester.getDepartment().getId().equals(targetDept.getId())) {
            throw new BusinessException("Kendi departmanınıza talep açamazsınız!");
        }

        // 3) Kategori gecerli ve hedef departmana ait olmali
        Category category = lookupService.findCategoryById(dto.getCategoryId())
                .orElseThrow(() -> new BusinessException(
                        "Seçilen kategori bulunamadı."));
        if (!category.getDepartment().getId().equals(targetDept.getId())) {
            throw new BusinessException(
                    "Seçtiğiniz kategori, hedef departmana ait değil!");
        }

        // 4) Oncelik gecerli mi
        Priority priority = lookupService.findPriorityById(dto.getPriorityId())
                .orElseThrow(() -> new BusinessException(
                        "Seçilen öncelik bulunamadı."));

        // 5) Talebi olustur
        Ticket ticket = new Ticket();
        ticket.setTitle(dto.getTitle().trim());
        ticket.setDescription(dto.getDescription().trim());
        ticket.setRequester(requester);
        ticket.setTargetDepartment(targetDept);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setStatus(lookupService.getOpenStatus());

        return ticketRepo.save(ticket);
    }


    @Transactional(readOnly = true)
    public Optional<Ticket> findById(Integer id) {
        return ticketRepo.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Ticket> findAll() {
        return ticketRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Ticket> findMyTickets(User requester) {
        return ticketRepo.findByRequesterOrderByCreatedAtDesc(requester);
    }

    @Transactional(readOnly = true)
    public List<Ticket> findByDepartmentId(Integer departmentId) {
        return ticketRepo.findByTargetDepartmentIdOrderByCreatedAtDesc(departmentId);
    }

    @Transactional(readOnly = true)
    public List<Ticket> findAssignedTo(User assignedUser) {
        return ticketRepo.findByAssignedUserOrderByCreatedAtDesc(assignedUser);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return ticketRepo.count();
    }

    @Transactional(readOnly = true)
    public long countByRequester(User requester) {
        return ticketRepo.countByRequester(requester);
    }

    @Transactional(readOnly = true)
    public long countByDepartmentId(Integer departmentId) {
        if (departmentId == null) {
            return 0;
        }
        return ticketRepo.countByTargetDepartmentId(departmentId);
    }

    @Transactional(readOnly = true)
    public long countAssignedTo(User assignedUser) {
        return ticketRepo.countByAssignedUser(assignedUser);
    }

    @Transactional(readOnly = true)
    public long countWaitingAssignmentInDepartment(Integer departmentId) {
        if (departmentId == null) {
            return 0;
        }
        return ticketRepo.countByTargetDepartmentIdAndAssignedUserIsNullAndStatusId(
                departmentId, TicketStatus.OPEN_ID);
    }

    @Transactional(readOnly = true)
    public long countResolvedThisMonthInDepartment(Integer departmentId) {
        if (departmentId == null) {
            return 0;
        }
        YearMonth month = YearMonth.now();
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(23, 59, 59);
        return ticketRepo.countByTargetDepartmentIdAndStatusIdAndClosedAtBetween(
                departmentId, TicketStatus.RESOLVED_ID, start, end);
    }

    @Transactional(readOnly = true)
    public long countOpenTickets() {
        return ticketRepo.countByStatusId(TicketStatus.OPEN_ID);
    }

    @Transactional(readOnly = true)
    public long countActiveAssignedTo(User assignedUser) {
        return ticketRepo.countByAssignedUserAndStatusIdIn(
                assignedUser, List.of(TicketStatus.OPEN_ID, TicketStatus.IN_PROGRESS_ID));
    }


    /**
     * Durum guncellemesi. Desktop'taki kurallar:
     *  - status 1..4 araliginda olmali
     *  - Cozulmus/Iptal edilmis talep tekrar guncellenemez
     *  - 3 (Cozuldu) veya 4 (Iptal) ise closed_at SIMDIKI an
     */
    @Transactional
    public Ticket updateStatus(Integer ticketId, Integer newStatusId) {
        if (newStatusId == null || newStatusId < 1 || newStatusId > 4) {
            throw new BusinessException("Geçersiz durum seçimi!");
        }

        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Talep bulunamadı."));

        if (isClosed(ticket)) {
            throw new BusinessException("Çözülmüş veya iptal edilmiş bir talep tekrar güncellenemez!");
        }

        TicketStatus newStatus = lookupService.findStatusById(newStatusId)
                .orElseThrow(() -> new BusinessException("Geçersiz durum seçimi!"));

        ticket.setStatus(newStatus);
        if (newStatusId.equals(TicketStatus.RESOLVED_ID)
                || newStatusId.equals(TicketStatus.CANCELLED_ID)) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        return ticketRepo.save(ticket);
    }

    /**
     * Talebi bir calisan'a atar. Desktop kurallari:
     *  - Talep kapali olmamali
     *  - Atanan kullanici DB'de olmali ve approved=true
     *  - Atanan kullanici Employee rolunde olmali
     *  - Atanan kullanici talebin HEDEF DEPARTMANINDA olmali
     */
    @Transactional
    public Ticket assignTicket(Integer ticketId, Integer assigneeUserId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Talep bulunamadı."));

        if (isClosed(ticket)) {
            throw new BusinessException("Çözülmüş veya iptal edilmiş bir talebe personel atanamaz!");
        }

        User assignee = userRepo.findById(assigneeUserId)
                .orElseThrow(() -> new BusinessException("Atanacak kullanıcı bulunamadı."));

        if (!assignee.isApproved()) {
            throw new BusinessException("Dondurulmuş veya onay bekleyen bir kullanıcıya talep atanamaz!");
        }
        if (assignee.getRole() == null || !assignee.getRole().getId().equals(Role.EMPLOYEE_ID)) {
            throw new BusinessException("Yalnızca 'Çalışan' rolündeki kullanıcılara atama yapılabilir.");
        }
        if (assignee.getDepartment() == null
                || !assignee.getDepartment().getId().equals(ticket.getTargetDepartment().getId())) {
            throw new BusinessException("Atanan çalışan, talebin hedef departmanında olmalıdır.");
        }

        ticket.setAssignedUser(assignee);
        // Atama yapildiginda durum Acik(1) ise Islemde(2)'ye yukseltilir.
        if (ticket.getStatus() != null
                && ticket.getStatus().getId().equals(TicketStatus.OPEN_ID)) {
            ticket.setStatus(lookupService.findStatusById(TicketStatus.IN_PROGRESS_ID)
                    .orElseThrow(() -> new BusinessException("Sistem hatası: İşlemde durumu bulunamadı.")));
        }
        return ticketRepo.save(ticket);
    }

    /**
     * Talebi siler. Cascade ile yanitlar da silinir (DB ON DELETE CASCADE).
     * Yetki kontrolu controller'da yapilir.
     */
    @Transactional
    public void deleteTicket(Integer ticketId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Talep bulunamadı."));
        ticketRepo.delete(ticket);
    }

    /**
     * Verili kullanicinin bu talebi gormeye yetkili olup olmadigini kontrol eder.
     * Yetkililer:
     *  - Talebi acan (requester)
     *  - Atanan calisan
     *  - Hedef departmandaki Manager
     *  - Admin
     */
    public boolean canView(Ticket ticket, User user) {
        if (user.getRole() == null) return false;
        Integer roleId = user.getRole().getId();
        if (Role.ADMIN_ID.equals(roleId)) return true;
        if (ticket.getRequester().getId().equals(user.getId())) return true;
        if (ticket.getAssignedUser() != null
                && ticket.getAssignedUser().getId().equals(user.getId())) return true;
        if (Role.MANAGER_ID.equals(roleId)
                && user.getDepartment() != null
                && user.getDepartment().getId().equals(ticket.getTargetDepartment().getId())) {
            return true;
        }
        return false;
    }

    public boolean canAssign(Ticket ticket, User user) {
        if (user.getRole() == null) return false;
        Integer roleId = user.getRole().getId();
        if (Role.ADMIN_ID.equals(roleId)) return true;
        return Role.MANAGER_ID.equals(roleId)
                && user.getDepartment() != null
                && user.getDepartment().getId().equals(ticket.getTargetDepartment().getId());
    }

    public boolean canChangeStatus(Ticket ticket, User user) {
        if (user.getRole() == null) return false;
        Integer roleId = user.getRole().getId();
        if (Role.ADMIN_ID.equals(roleId)) return true;
        if (Role.MANAGER_ID.equals(roleId)
                && user.getDepartment() != null
                && user.getDepartment().getId().equals(ticket.getTargetDepartment().getId())) return true;
        // Atanmis calisan sadece 3 (Cozuldu) veya 4 (Iptal)'e gecirebilir; bu kural
        // controller tarafindan ek olarak dogrulanir.
        return Role.EMPLOYEE_ID.equals(roleId)
                && ticket.getAssignedUser() != null
                && ticket.getAssignedUser().getId().equals(user.getId());
    }

    public boolean canDelete(User user) {
        return user.getRole() != null && Role.ADMIN_ID.equals(user.getRole().getId());
    }

    /**
     * Talebi acan kullanici, kapatilmamis talebini iptal edebilir.
     * Atanmis calisan varsa iptal sonrasi atama korunur.
     */
    public boolean canCancelByRequester(Ticket ticket, User user) {
        if (user == null || ticket.getRequester() == null) {
            return false;
        }
        if (!ticket.getRequester().getId().equals(user.getId())) {
            return false;
        }
        return !isClosed(ticket);
    }

    @Transactional
    public Ticket cancelByRequester(Integer ticketId, User requester) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new BusinessException("Talep bulunamadı."));

        if (!canCancelByRequester(ticket, requester)) {
            throw new BusinessException(
                    "Bu talebi iptal edemezsiniz. Yalnızca kendi açık taleplerinizi iptal edebilirsiniz.");
        }

        TicketStatus cancelled = lookupService.findStatusById(TicketStatus.CANCELLED_ID)
                .orElseThrow(() -> new BusinessException("Geçersiz durum seçimi!"));

        ticket.setStatus(cancelled);
        ticket.setClosedAt(LocalDateTime.now());
        return ticketRepo.save(ticket);
    }

    private boolean isClosed(Ticket ticket) {
        Integer sid = ticket.getStatus() != null ? ticket.getStatus().getId() : null;
        return sid != null
                && (sid.equals(TicketStatus.RESOLVED_ID) || sid.equals(TicketStatus.CANCELLED_ID));
    }

    /**
     * Servis katmaninda firlatilan domain hata sinifi. Controller bunu yakalayip
     * BindingResult veya flash mesaj olarak ekrana yansitir.
     */
    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }
}
