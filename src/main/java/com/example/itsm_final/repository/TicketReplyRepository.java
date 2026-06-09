package com.example.itsm_final.repository;

import com.example.itsm_final.model.Ticket;
import com.example.itsm_final.model.TicketReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketReplyRepository extends JpaRepository<TicketReply, Integer> {

    List<TicketReply> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
