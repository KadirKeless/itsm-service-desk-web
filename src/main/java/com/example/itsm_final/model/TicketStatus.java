package com.example.itsm_final.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Talep durumu lookup tablosu.
 *
 * NOT: DB semasinda `id INT PRIMARY KEY` (AUTO_INCREMENT YOK). Bu yuzden ID
 * otomatik uretilmez; uygulama tarafindan SABIT olarak verilir. Sabitler asagidaki
 * static alanlarda tanimli (OPEN_ID, IN_PROGRESS_ID, ...). DataInitializer bu
 * sabitleri kullanarak seed yapar.
 */
@Entity
@Table(name = "ticket_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatus {

    public static final Integer OPEN_ID = 1;          // Acik
    public static final Integer IN_PROGRESS_ID = 2;   // Islemde
    public static final Integer RESOLVED_ID = 3;      // Cozuldu
    public static final Integer CANCELLED_ID = 4;     // Iptal Edildi

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "status_name", nullable = false, length = 50)
    private String statusName;

    @Override
    public String toString() {
        return statusName;
    }
}
