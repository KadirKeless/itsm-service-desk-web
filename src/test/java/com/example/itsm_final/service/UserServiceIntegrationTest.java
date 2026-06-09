package com.example.itsm_final.service;

import com.example.itsm_final.dto.CreateTicketDto;
import com.example.itsm_final.dto.RegisterDto;
import com.example.itsm_final.model.User;
import com.example.itsm_final.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketService ticketService;

    @Test
    @DisplayName("Senaryo 1: Yeni Kullanıcı Kaydı ve Veritabanı Kalıcılık Testi")
    void testRegisterUserAndKeepInDatabase() {
        long timestamp = System.currentTimeMillis();
        String testEmail = "gozlem_testi_" + timestamp + "@isikun.com";

        try {
            RegisterDto dto = new RegisterDto();
            dto.setFirstName("Gözlem");
            dto.setLastName("Testi");
            dto.setEmail(testEmail);
            dto.setPassword("Gecerli!123");

            User savedUser = userService.register(dto);
            assertNotNull(savedUser, "Kullanıcı veritabanına başarıyla kaydedilmelidir.");

            User found = userRepository.findByEmail(testEmail.toLowerCase()).orElse(null);
            assertNotNull(found, "Kaydedilen kullanıcı veritabanında bulunabilmelidir.");
            assertFalse(found.isApproved(), "Yeni kullanıcı varsayılan olarak onaysız (0) olmalıdır.");
        } catch (Exception e) {
            fail("Beklenmeyen bir hata oluştu: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Senaryo 2: Talep Oluşturma ve Veritabanı Kaydı")
    void testCreateTicketWithLogicalMatch() {
        long timestamp = System.currentTimeMillis();
        String ticketTitle = "[OTOMASYON TESTİ] - " + timestamp;
        String ticketDesc = "Bilgi Teknolojileri birimine alınacak yeni yazılımcı için işe alım talebidir.";

        try {
            User requester = userRepository.findById(1)
                    .orElseThrow(() -> new IllegalStateException("Test için kullanıcı #1 bulunamadı."));

            CreateTicketDto dto = new CreateTicketDto();
            dto.setTitle(ticketTitle);
            dto.setDescription(ticketDesc);
            dto.setTargetDepartmentId(2);
            dto.setCategoryId(11);
            dto.setPriorityId(1);

            var ticket = ticketService.createTicket(dto, requester);
            assertNotNull(ticket.getId(), "Talep veritabanına kaydedilmelidir.");
        } catch (TicketService.BusinessException ex) {
            fail("Talep oluşturulurken hata alındı: " + ex.getMessage());
        } catch (Exception e) {
            fail("Talep oluşturma testinde beklenmedik hata: " + e.getMessage());
        }
    }
}
