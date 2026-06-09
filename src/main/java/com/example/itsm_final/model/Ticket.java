package com.example.itsm_final.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ITSM ana entity'si: kullanicilarin actigi destek talepleri.
 *
 * Iliski haritasi:
 *   - requester: talebi acan kullanici (User)
 *   - targetDepartment: talebin yonlendirildigi departman (Department)
 *   - category: talep kategorisi (Category - departmana bagli)
 *   - priority: oncelik seviyesi (Priority)
 *   - status: durum (TicketStatus: Acik / Islemde / Cozuldu / Iptal)
 *   - assignedUser: talebi ustlenen calisan (User, opsiyonel)
 *
 * Zaman damgalari:
 *   - createdAt: Hibernate tarafindan otomatik (INSERT)
 *   - updatedAt: Hibernate tarafindan otomatik (UPDATE)
 *   - closedAt: status COZULDU/IPTAL'e gecince servis katmaninda set edilir
 */
@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Başlık zorunludur")
    @Size(min = 5, max = 100, message = "Başlık 5-100 karakter arasında olmalıdır")
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @NotBlank(message = "Açıklama zorunludur")
    @Size(min = 10, max = 2000, message = "Açıklama 10-2000 karakter arasında olmalıdır")
    @Lob
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "target_department_id", nullable = false)
    private Department targetDepartment;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "priority_id", nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private TicketStatus status;

    /** Atanan calisan; talep bekleme listesindeyken null kalabilir. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Talep kapandiginda servis katmaninda manuel set edilir. */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
