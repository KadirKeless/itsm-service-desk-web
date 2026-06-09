package com.example.itsm_final.repository;

import com.example.itsm_final.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketStatusRepository extends JpaRepository<TicketStatus, Integer> {

    List<TicketStatus> findAllByOrderByIdAsc();

    Optional<TicketStatus> findByStatusName(String statusName);
}
