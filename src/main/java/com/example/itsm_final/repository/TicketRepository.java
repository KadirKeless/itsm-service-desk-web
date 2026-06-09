package com.example.itsm_final.repository;

import com.example.itsm_final.model.Ticket;
import com.example.itsm_final.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {

    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket> findByRequesterOrderByCreatedAtDesc(User requester);

    List<Ticket> findByTargetDepartmentIdOrderByCreatedAtDesc(Integer departmentId);

    long countByTargetDepartmentId(Integer departmentId);

    long countByTargetDepartmentIdAndAssignedUserIsNullAndStatusId(
            Integer departmentId, Integer statusId);

    long countByTargetDepartmentIdAndStatusIdAndClosedAtBetween(
            Integer departmentId, Integer statusId,
            LocalDateTime closedAtStart, LocalDateTime closedAtEnd);

    long countByStatusId(Integer statusId);

    List<Ticket> findByAssignedUserOrderByCreatedAtDesc(User assignedUser);

    long countByRequester(User requester);

    long countByAssignedUser(User assignedUser);

    /** Dondurma kontrolu icin: kullaniciya atanan ve hala Acik(1)/Islemde(2) talepler. */
    long countByAssignedUserAndStatusIdIn(User assignedUser, List<Integer> statusIds);
}
