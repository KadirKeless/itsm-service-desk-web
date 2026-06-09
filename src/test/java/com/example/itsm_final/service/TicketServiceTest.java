package com.example.itsm_final.service;

import com.example.itsm_final.model.Ticket;
import com.example.itsm_final.model.TicketStatus;
import com.example.itsm_final.model.User;
import com.example.itsm_final.repository.TicketRepository;
import com.example.itsm_final.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepo;

    @Mock
    private LookupService lookupService;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void testUpdateTicketStatusInvalidStatusIdZero() {
        TicketService.BusinessException ex = assertThrows(
                TicketService.BusinessException.class,
                () -> ticketService.updateStatus(1, 0));
        assertEquals("Geçersiz durum seçimi!", ex.getMessage());
    }

    @Test
    void testUpdateTicketStatusInvalidStatusIdTooHigh() {
        TicketService.BusinessException ex = assertThrows(
                TicketService.BusinessException.class,
                () -> ticketService.updateStatus(1, 5));
        assertEquals("Geçersiz durum seçimi!", ex.getMessage());
    }

    @Test
    void testUpdateTicketStatusInvalidStatusNegative() {
        TicketService.BusinessException ex = assertThrows(
                TicketService.BusinessException.class,
                () -> ticketService.updateStatus(99, -1));
        assertEquals("Geçersiz durum seçimi!", ex.getMessage());
    }

    @Test
    void cancelByRequesterSetsCancelledStatusAndClosedAtWhenUnassigned() {
        User requester = new User();
        requester.setId(5);

        TicketStatus openStatus = new TicketStatus();
        openStatus.setId(TicketStatus.OPEN_ID);

        TicketStatus cancelledStatus = new TicketStatus();
        cancelledStatus.setId(TicketStatus.CANCELLED_ID);
        cancelledStatus.setStatusName("İptal Edildi");

        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setRequester(requester);
        ticket.setStatus(openStatus);

        when(ticketRepo.findById(1)).thenReturn(Optional.of(ticket));
        when(lookupService.findStatusById(TicketStatus.CANCELLED_ID)).thenReturn(Optional.of(cancelledStatus));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket result = ticketService.cancelByRequester(1, requester);

        assertEquals(TicketStatus.CANCELLED_ID, result.getStatus().getId());
        assertNull(result.getAssignedUser());
        assertNotNull(result.getClosedAt());
    }

    @Test
    void cancelByRequesterKeepsAssigneeWhenAssigned() {
        User requester = new User();
        requester.setId(5);

        User assignee = new User();
        assignee.setId(9);

        TicketStatus inProgressStatus = new TicketStatus();
        inProgressStatus.setId(TicketStatus.IN_PROGRESS_ID);

        TicketStatus cancelledStatus = new TicketStatus();
        cancelledStatus.setId(TicketStatus.CANCELLED_ID);
        cancelledStatus.setStatusName("İptal Edildi");

        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setRequester(requester);
        ticket.setAssignedUser(assignee);
        ticket.setStatus(inProgressStatus);

        when(ticketRepo.findById(1)).thenReturn(Optional.of(ticket));
        when(lookupService.findStatusById(TicketStatus.CANCELLED_ID)).thenReturn(Optional.of(cancelledStatus));
        when(ticketRepo.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

        Ticket result = ticketService.cancelByRequester(1, requester);

        assertEquals(TicketStatus.CANCELLED_ID, result.getStatus().getId());
        assertEquals(9, result.getAssignedUser().getId());
        assertNotNull(result.getClosedAt());
    }

    @Test
    void cancelByRequesterFailsWhenNotRequester() {
        User requester = new User();
        requester.setId(5);

        User otherUser = new User();
        otherUser.setId(99);

        TicketStatus openStatus = new TicketStatus();
        openStatus.setId(TicketStatus.OPEN_ID);

        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setRequester(requester);
        ticket.setStatus(openStatus);

        when(ticketRepo.findById(1)).thenReturn(Optional.of(ticket));

        assertThrows(TicketService.BusinessException.class,
                () -> ticketService.cancelByRequester(1, otherUser));
    }
}
