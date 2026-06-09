package com.example.itsm_final.service;

import com.example.itsm_final.dto.AdminCreateUserDto;
import com.example.itsm_final.repository.DepartmentRepository;
import com.example.itsm_final.repository.RoleRepository;
import com.example.itsm_final.repository.TicketRepository;
import com.example.itsm_final.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProfilePictureService profilePictureService;

    @InjectMocks
    private UserService userService;

    @Test
    void testAdminCreateUserFailsWhenRoleIsAdmin() {
        AdminCreateUserDto dto = new AdminCreateUserDto();
        dto.setFirstName("Ad");
        dto.setLastName("Soyad");
        dto.setEmail("a@b.com");
        dto.setPassword("Valid1!pass");
        dto.setRoleId(1);
        dto.setDepartmentId(1);

        UserService.BusinessException ex = assertThrows(
                UserService.BusinessException.class,
                () -> userService.adminCreateUser(dto));
        assertEquals("Admin rolü yeni kullanıcılara atanamaz!", ex.getMessage());
    }

    @Test
    void testUpdateProfilePictureFailsWhenFileTooLarge(@TempDir Path tempDir) throws Exception {
        ProfilePictureService pictureService = new ProfilePictureService(tempDir.toString());

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(5L * 1024 * 1024 + 1);

        UserService.BusinessException ex = assertThrows(
                UserService.BusinessException.class,
                () -> pictureService.processUpload(file));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("5 MB"), "Beklenen boyut uyarısı.");
    }
}
