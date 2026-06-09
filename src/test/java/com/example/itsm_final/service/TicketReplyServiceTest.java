package com.example.itsm_final.service;

import com.example.itsm_final.model.Ticket;
import com.example.itsm_final.model.TicketReply;
import com.example.itsm_final.model.TicketStatus;
import com.example.itsm_final.model.User;
import com.example.itsm_final.repository.TicketReplyRepository;
import com.example.itsm_final.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketReplyServiceTest {

    @Mock
    private TicketReplyRepository replyRepo;

    @Mock
    private TicketRepository ticketRepo;

    @InjectMocks
    private TicketReplyService replyService;

    private Ticket openTicketStubbed() {
        TicketStatus openStatus = new TicketStatus();
        openStatus.setId(TicketStatus.OPEN_ID);

        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setStatus(openStatus);

        when(ticketRepo.findById(1)).thenReturn(Optional.of(ticket));
        return ticket;
    }

    @Test
    void testAddReplyTooShortMessage() {
        openTicketStubbed();
        TicketReplyService.ReplyException ex = assertThrows(
                TicketReplyService.ReplyException.class,
                () -> replyService.addReply(1, "ab", new User()));
        assertEquals("Yanıt mesajı en az 3 karakter olmalıdır!", ex.getMessage());
    }

    @Test
    void testAddReplyNullMessage() {
        openTicketStubbed();
        TicketReplyService.ReplyException ex = assertThrows(
                TicketReplyService.ReplyException.class,
                () -> replyService.addReply(1, null, new User()));
        assertEquals("Yanıt mesajı en az 3 karakter olmalıdır!", ex.getMessage());
    }

    @Test
    void testAddReplyWhitespaceOnly() {
        openTicketStubbed();
        TicketReplyService.ReplyException ex = assertThrows(
                TicketReplyService.ReplyException.class,
                () -> replyService.addReply(1, "  ", new User()));
        assertEquals("Yanıt mesajı en az 3 karakter olmalıdır!", ex.getMessage());
    }

    @Test
    void testUpdateReplyTooShort() {
        TicketStatus openStatus = new TicketStatus();
        openStatus.setId(TicketStatus.OPEN_ID);

        Ticket ticket = new Ticket();
        ticket.setId(1);
        ticket.setStatus(openStatus);

        User author = new User();
        author.setId(1);

        TicketReply reply = new TicketReply();
        reply.setId(1);
        reply.setTicket(ticket);
        reply.setUser(author);

        when(replyRepo.findById(1)).thenReturn(Optional.of(reply));

        TicketReplyService.ReplyException ex = assertThrows(
                TicketReplyService.ReplyException.class,
                () -> replyService.updateReply(1, "xx", author, false));
        assertEquals("Yanıt mesajı en az 3 karakter olmalıdır!", ex.getMessage());
    }
}
