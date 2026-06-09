package com.example.itsm_final.service;

import com.example.itsm_final.dto.AdminCreateUserDto;
import com.example.itsm_final.dto.AdminResetPasswordDto;
import com.example.itsm_final.dto.AdminUpdateUserDto;
import com.example.itsm_final.dto.ChangePasswordDto;
import com.example.itsm_final.dto.RegisterDto;
import com.example.itsm_final.model.Department;
import com.example.itsm_final.model.Role;
import com.example.itsm_final.model.TicketStatus;
import com.example.itsm_final.model.User;
import com.example.itsm_final.repository.DepartmentRepository;
import com.example.itsm_final.repository.RoleRepository;
import com.example.itsm_final.repository.TicketRepository;
import com.example.itsm_final.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Kullanici islemlerinin sahibi olan servis. Tum kayitli is kurallari:
 *
 *  - register: yeni kayit (rol/dep null, approved=false). Sifre BCrypt'le hashlenir.
 *  - changePassword: mevcut sifre dogru olmali, yeni sifre eski ile ayni olamaz.
 *  - admin operasyonlari:
 *      activateUser: ilk onay (rol+dep gerekir) veya geri-acma (eski rol/dep).
 *                    Manager kotasi: departman basina 1. Employee kotasi: 5.
 *                    Admin rolu yeni kullaniciya verilemez.
 *      freezeUser: aktif Acik/Islemde atanmis talebi olan kullanici dondurulamaz.
 *                  Admin hesabi dondurulamaz.
 *      deleteUser: Admin silinemez. FK kisitlari nedeniyle talebi olan kullanici silinemez.
 *      updateUser: ad/soyad/email/rol/dep guncelle; rol/dep degistiyse kota kontrolu.
 *      adminCreateUser: direkt onayli (approved=true) kullanici olusturur.
 *      updateProfilePicture: profil resmi yolu guncellenir (dosya islemleri ayri akista).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfilePictureService profilePictureService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       DepartmentRepository departmentRepository,
                       TicketRepository ticketRepository,
                       PasswordEncoder passwordEncoder,
                       ProfilePictureService profilePictureService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
        this.profilePictureService = profilePictureService;
    }


    @Transactional
    public User register(RegisterDto dto) {
        String email = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(
                    "Bu e-posta adresi zaten kayıtlı: " + email);
        }

        User user = new User();
        user.setFirstName(dto.getFirstName().trim());
        user.setLastName(dto.getLastName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setApproved(false);
        return userRepository.save(user);
    }


    /**
     * Kullanici kendi sifresini degistirir. Mevcut sifre BCrypt karsilastirilir.
     * Eski sifre plain-text ise (henuz migrate edilmemis) onu da destekleriz.
     */
    @Transactional
    public void changePassword(Integer userId, ChangePasswordDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı."));

        // Mevcut sifre kontrolu: hem BCrypt hem plain-text legacy
        boolean matches = passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())
                || dto.getCurrentPassword().equals(user.getPassword());
        if (!matches) {
            throw new BusinessException("Mevcut şifrenizi yanlış girdiniz!");
        }

        if (dto.getCurrentPassword().equals(dto.getNewPassword())) {
            throw new BusinessException("Yeni şifre, mevcut şifre ile aynı olamaz!");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("Yeni şifreler birbiriyle eşleşmiyor!");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    /** Admin: kullanicinin sifresini mevcut sifreyi bilmeden gunceller. */
    @Transactional
    public void adminResetPassword(Integer userId, AdminResetPasswordDto dto) {
        User user = getById(userId);
        if (user.getRole() != null && Role.ADMIN_ID.equals(user.getRole().getId())) {
            throw new BusinessException("Sistem yöneticisi şifresi bu ekrandan değiştirilemez.");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("Yeni şifreler birbiriyle eşleşmiyor!");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }


    @Transactional
    public void updateProfilePicture(Integer userId, org.springframework.web.multipart.MultipartFile file) {
        User user = getById(userId);
        try {
            profilePictureService.deleteFile(userId, user.getProfilePicture());
            String path = profilePictureService.saveForUser(userId, file);
            user.setProfilePicture(path);
            userRepository.save(user);
        } catch (java.io.IOException ex) {
            throw new BusinessException("Gorsel sisteme kaydedilirken hata olustu: " + ex.getMessage());
        }
    }

    @Transactional
    public void removeProfilePicture(Integer userId) {
        User user = getById(userId);
        profilePictureService.deleteFile(userId, user.getProfilePicture());
        user.setProfilePicture(null);
        userRepository.save(user);
    }

    /** Avatar gosterimi icin: dosya var mi kontrol et. */
    @Transactional(readOnly = true)
    public boolean hasProfilePicture(User user) {
        return user.getProfilePicture() != null
                && profilePictureService.fileExists(user.getProfilePicture());
    }


    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public List<User> getUnapprovedUsers() {
        return userRepository.findByApprovedFalseAndRoleIsNullAndDepartmentIsNullOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public long countPendingApprovalUsers() {
        return userRepository.countByApprovedFalseAndRoleIsNullAndDepartmentIsNull();
    }

    @Transactional(readOnly = true)
    public User getById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı."));
    }


    @Transactional
    public void activateUser(Integer userId, Integer roleId, Integer departmentId) {
        User target = getById(userId);
        if (target.isApproved()) {
            throw new BusinessException("Bu kullanıcı zaten aktif durumda!");
        }

        boolean needsAssignment = (target.getRole() == null || target.getDepartment() == null);

        Integer effectiveRoleId;
        Integer effectiveDeptId;

        if (needsAssignment) {
            if (roleId == null || departmentId == null) {
                throw new BusinessException("Bu kullanıcıya önce bir rol ve departman atamalısınız!");
            }
            if (Role.ADMIN_ID.equals(roleId)) {
                throw new BusinessException("Admin rolü yeni kullanıcılara atanamaz!");
            }
            effectiveRoleId = roleId;
            effectiveDeptId = departmentId;
            target.setRole(loadRole(roleId));
            target.setDepartment(loadDepartment(departmentId));
        } else {
            effectiveRoleId = target.getRole().getId();
            effectiveDeptId = target.getDepartment().getId();
        }

        enforceQuota(effectiveRoleId, effectiveDeptId);
        target.setApproved(true);
        userRepository.save(target);
    }


    @Transactional
    public void freezeUser(Integer userId) {
        User target = getById(userId);
        if (!target.isApproved()) {
            throw new BusinessException("Bu kullanıcı zaten pasif durumda!");
        }
        if (target.getRole() != null && Role.ADMIN_ID.equals(target.getRole().getId())) {
            throw new BusinessException("Admin hesabı dondurulamaz!");
        }
        long activeAssigned = ticketRepository.countByAssignedUserAndStatusIdIn(
                target, List.of(TicketStatus.OPEN_ID, TicketStatus.IN_PROGRESS_ID));
        if (activeAssigned > 0) {
            throw new BusinessException(
                    "Bu kullanıcıya atanmış ve hâlen Açık veya İşlemde olan "
                            + activeAssigned + " talep var. Önce bunları Çözüldü/İptal yapınız.");
        }
        target.setApproved(false);
        userRepository.save(target);
    }


    @Transactional
    public void deleteUser(Integer userId) {
        User target = getById(userId);
        validateDeletable(target);
        try {
            userRepository.delete(target);
            userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(
                    "Kullanıcı silinemedi: bu kullanıcıya ait talepler bulunuyor.");
        }
    }


    /** Masaustu AdminPanelUI ile ayni durum kodlari. */
    public static String resolveStatusKey(User user) {
        if (user.isApproved()) return "active";
        if (user.getRole() == null && user.getDepartment() == null) return "pending";
        return "frozen";
    }

    public static String resolveStatusLabel(User user) {
        return switch (resolveStatusKey(user)) {
            case "active" -> "Aktif";
            case "pending" -> "Onay Bekliyor";
            default -> "Dondurulmuş";
        };
    }

    @Transactional(readOnly = true)
    public void validateBulkActivate(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("Lütfen işlem yapmak için en az bir kullanıcı seçiniz!");
        }
        List<User> users = loadUsersByIds(userIds);
        for (User u : users) {
            if (u.isApproved()) {
                if (userIds.size() == 1) {
                    throw new BusinessException(
                            u.getFullName() + " zaten Aktif durumda.\n'Dondurulmuş' veya 'Onay Bekliyor' durumundaki bir hesap seçiniz.");
                }
                throw new BusinessException(
                        "Seçili kullanıcılar arasında zaten Aktif olanlar bulunuyor.\nToplu işlem için lütfen sadece 'Dondurulmuş' veya 'Onay Bekliyor' durumundaki hesapları seçiniz.");
            }
        }
    }

    @Transactional
    public int bulkActivateUsers(List<Integer> userIds, Integer roleId, Integer departmentId) {
        validateBulkActivate(userIds);
        int success = 0;
        String lastError = null;
        for (Integer id : userIds) {
            try {
                activateUser(id, roleId, departmentId);
                success++;
            } catch (BusinessException ex) {
                lastError = ex.getMessage();
            }
        }
        if (success == 0 && lastError != null) {
            throw new BusinessException(lastError);
        }
        if (success < userIds.size() && lastError != null) {
            throw new BusinessException(
                    success + " kullanıcı aktifleştirildi. "
                            + (userIds.size() - success) + " kullanıcıda hata oluştu: " + lastError);
        }
        return success;
    }

    @Transactional(readOnly = true)
    public void validateBulkFreeze(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("Lütfen işlem yapmak için en az bir kullanıcı seçiniz!");
        }
        List<User> users = loadUsersByIds(userIds);
        for (User u : users) {
            if (!u.isApproved()) {
                if (userIds.size() == 1) {
                    throw new BusinessException(
                            u.getFullName() + " zaten pasif durumda.\nLütfen 'Aktif' bir hesap seçiniz.");
                }
                throw new BusinessException(
                        "Seçili kullanıcılar arasında zaten pasif (dondurulmuş veya onay bekleyen) durumda olanlar var.\nToplu işlem için lütfen SADECE 'Aktif' kullanıcıları seçiniz.");
            }
            if (u.getRole() != null && Role.ADMIN_ID.equals(u.getRole().getId())) {
                throw new BusinessException(userIds.size() == 1
                        ? "Admin hesabı dondurulamaz."
                        : "Seçili kullanıcılar arasında Admin hesabı bulunuyor! Admin hesapları dondurulamaz.");
            }
        }
    }

    @Transactional
    public int bulkFreezeUsers(List<Integer> userIds) {
        validateBulkFreeze(userIds);
        int success = 0;
        String lastError = null;
        for (Integer id : userIds) {
            try {
                freezeUser(id);
                success++;
            } catch (BusinessException ex) {
                lastError = ex.getMessage();
            }
        }
        if (success == 0 && lastError != null) {
            throw new BusinessException(lastError);
        }
        if (success < userIds.size() && lastError != null) {
            throw new BusinessException(
                    success + " kullanıcı donduruldu. "
                            + (userIds.size() - success) + " kullanıcıda işlem yapılamadı: " + lastError);
        }
        return success;
    }

    @Transactional(readOnly = true)
    public void validateBulkDelete(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new BusinessException("Lütfen işlem yapmak için en az bir kullanıcı seçiniz!");
        }
        List<User> users = loadUsersByIds(userIds);
        for (User u : users) {
            if (u.getRole() != null && Role.ADMIN_ID.equals(u.getRole().getId())) {
                throw new BusinessException(userIds.size() == 1
                        ? "Admin hesabı sistemden silinemez."
                        : "Seçilen kullanıcılar arasında Admin hesabı bulunuyor! Admin hesapları sistemden silinemez.");
            }
            if (!canDeleteUser(u)) {
                if (userIds.size() == 1) {
                    throw new BusinessException(
                            u.getFullName() + " silinemedi (mevcut talepleri bulunuyor).");
                }
                throw new BusinessException(
                        "Seçilen kullanıcıların tümü silme işlemine uygun değil.\nTalebi bulunan veya silinemeyecek hesapları listeden çıkarınız.");
            }
        }
    }

    @Transactional
    public int bulkDeleteUsers(List<Integer> userIds) {
        validateBulkDelete(userIds);
        int success = 0;
        for (Integer id : userIds) {
            deleteUser(id);
            success++;
        }
        return success;
    }

    @Transactional(readOnly = true)
    public boolean canDeleteUser(User user) {
        if (user.getRole() != null && Role.ADMIN_ID.equals(user.getRole().getId())) {
            return false;
        }
        long asRequester = ticketRepository.countByRequester(user);
        long asAssignee = ticketRepository.countByAssignedUser(user);
        return asRequester == 0 && asAssignee == 0;
    }

    private void validateDeletable(User user) {
        if (user.getRole() != null && Role.ADMIN_ID.equals(user.getRole().getId())) {
            throw new BusinessException("Admin hesabı silinemez!");
        }
        if (!canDeleteUser(user)) {
            throw new BusinessException(
                    "Kullanıcı silinemedi: bu kullanıcıya ait talepler bulunuyor.");
        }
    }

    private List<User> loadUsersByIds(List<Integer> userIds) {
        return userIds.stream().map(this::getById).toList();
    }


    @Transactional
    public void updateUser(Integer userId, AdminUpdateUserDto dto) {
        User target = getById(userId);

        if (target.getRole() != null && Role.ADMIN_ID.equals(target.getRole().getId())) {
            throw new BusinessException("Admin hesabı üzerinde değişiklik yapılamaz!");
        }
        if (Role.ADMIN_ID.equals(dto.getRoleId())) {
            throw new BusinessException("Admin rolü atanamaz!");
        }

        // Email degistiyse benzersizlik kontrolu
        String newEmail = dto.getEmail().trim().toLowerCase();
        if (!newEmail.equals(target.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new BusinessException("Bu e-posta zaten başka bir kullanıcıya ait.");
        }

        // Rol veya departman degistiyse kota kontrolu
        Integer currentRoleId = target.getRole() != null ? target.getRole().getId() : null;
        Integer currentDeptId = target.getDepartment() != null ? target.getDepartment().getId() : null;
        boolean roleChanged = !Objects.equals(dto.getRoleId(), currentRoleId);
        boolean deptChanged = !Objects.equals(dto.getDepartmentId(), currentDeptId);
        if (roleChanged || deptChanged) {
            enforceQuota(dto.getRoleId(), dto.getDepartmentId());
        }

        target.setFirstName(dto.getFirstName().trim());
        target.setLastName(dto.getLastName().trim());
        target.setEmail(newEmail);
        target.setRole(loadRole(dto.getRoleId()));
        target.setDepartment(loadDepartment(dto.getDepartmentId()));
        userRepository.save(target);
    }


    @Transactional
    public User adminCreateUser(AdminCreateUserDto dto) {
        if (Role.ADMIN_ID.equals(dto.getRoleId())) {
            throw new BusinessException("Admin rolü yeni kullanıcılara atanamaz!");
        }
        String email = dto.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Bu e-posta adresi zaten kayıtlı.");
        }
        enforceQuota(dto.getRoleId(), dto.getDepartmentId());

        User u = new User();
        u.setFirstName(dto.getFirstName().trim());
        u.setLastName(dto.getLastName().trim());
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(dto.getPassword()));
        u.setRole(loadRole(dto.getRoleId()));
        u.setDepartment(loadDepartment(dto.getDepartmentId()));
        u.setApproved(true);
        return userRepository.save(u);
    }


    private Role loadRole(Integer roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("Geçerli bir rol seçiniz."));
    }

    private Department loadDepartment(Integer deptId) {
        return departmentRepository.findById(deptId)
                .orElseThrow(() -> new BusinessException("Geçerli bir departman seçiniz."));
    }

    /**
     * Manager (rol=2) icin departman basina max 1; Employee (rol=3) icin max 5.
     * Admin (rol=1) ayri kanal; bu metoddan gecmez (uste cagrilar kontrol ediyor).
     */
    private void enforceQuota(Integer roleId, Integer departmentId) {
        if (Role.MANAGER_ID.equals(roleId)) {
            long c = userRepository.countByDepartmentIdAndRoleIdAndApprovedTrue(
                    departmentId, Role.MANAGER_ID);
            if (c >= 1) {
                throw new BusinessException(
                        "Bu departmanda zaten bir Yönetici bulunuyor! (Maksimum: 1)");
            }
        } else if (Role.EMPLOYEE_ID.equals(roleId)) {
            long c = userRepository.countByDepartmentIdAndRoleIdAndApprovedTrue(
                    departmentId, Role.EMPLOYEE_ID);
            if (c >= 5) {
                throw new BusinessException(
                        "Bu departmanda çalışan kapasitesi dolu! (Maksimum: 5)");
            }
        }
    }


    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }
}
