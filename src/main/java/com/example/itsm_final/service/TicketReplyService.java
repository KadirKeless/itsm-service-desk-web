package com.example.itsm_final.service;

import com.example.itsm_final.model.Ticket;
import com.example.itsm_final.model.TicketReply;
import com.example.itsm_final.model.TicketStatus;
import com.example.itsm_final.model.User;
import com.example.itsm_final.repository.TicketReplyRepository;
import com.example.itsm_final.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Talep yanit/yorum is mantigini yoneten servis.
 *
 * Masaustu TicketReplyService'ten asagidaki kurallar tasinmistir:
 *  - Yanit mesaji min 3 karakter (trimmed)
 *  - Cozulmus/Iptal edilmis talebe yanit YAZILAMAZ (kapali ticket kilidi)
 *  - Sadece YORUM SAHIBI veya ADMIN yorumu duzenleyebilir/silebilir
 *  - Kapali talepte yorum duzenleme/silme yalniz ADMIN tarafindan yapilabilir
 */
@Service
public class TicketReplyService {

    private final TicketReplyRepository replyRepo;
    private final TicketRepository ticketRepo;

    public TicketReplyService(TicketReplyRepository replyRepo, TicketRepository ticketRepo) {
        this.replyRepo = replyRepo;
        this.ticketRepo = ticketRepo;
    }

    @Transactional(readOnly = true)
    public List<TicketReply> getRepliesForTicket(Ticket ticket) {
        return replyRepo.findByTicketOrderByCreatedAtAsc(ticket);
    }

    @Transactional
    public TicketReply addReply(Integer ticketId, String message, User author) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ReplyException("Talep bulunamadı."));

        if (isClosed(ticket)) {
            throw new ReplyException("Çözülmüş veya iptal edilmiş talebe yanıt yazılamaz.");
        }

        String trimmed = message == null ? "" : message.trim();
        if (trimmed.length() < 3) {
            throw new ReplyException("Yanıt mesajı en az 3 karakter olmalıdır!");
        }

        TicketReply reply = new TicketReply();
        reply.setTicket(ticket);
        reply.setUser(author);
        reply.setMessage(trimmed);
        return replyRepo.save(reply);
    }

    @Transactional
    public void updateReply(Integer replyId, String newMessage, User actor, boolean actorIsAdmin) {
        TicketReply reply = replyRepo.findById(replyId)
                .orElseThrow(() -> new ReplyException("Yanıt bulunamadı."));

        Ticket ticket = reply.getTicket();
        boolean isAuthor = reply.getUser().getId().equals(actor.getId());

        if (!isAuthor && !actorIsAdmin) {
            throw new ReplyException("Bu yorumu düzenleme yetkiniz yok.");
        }
        if (isClosed(ticket) && !actorIsAdmin) {
            throw new ReplyException("Çözülmüş veya iptal edilmiş taleplerde yorum düzenlenemez.");
        }

        String trimmed = newMessage == null ? "" : newMessage.trim();
        if (trimmed.length() < 3) {
            throw new ReplyException("Yanıt mesajı en az 3 karakter olmalıdır!");
        }

        reply.setMessage(trimmed);
        replyRepo.save(reply);
    }

    @Transactional
    public void deleteReply(Integer replyId, User actor, boolean actorIsAdmin) {
        TicketReply reply = replyRepo.findById(replyId)
                .orElseThrow(() -> new ReplyException("Yanıt bulunamadı."));

        Ticket ticket = reply.getTicket();
        boolean isAuthor = reply.getUser().getId().equals(actor.getId());

        if (!isAuthor && !actorIsAdmin) {
            throw new ReplyException("Bu yorumu silme yetkiniz yok.");
        }
        if (isClosed(ticket) && !actorIsAdmin) {
            throw new ReplyException("Çözülmüş veya iptal edilmiş taleplerde yorum silinemez.");
        }

        replyRepo.delete(reply);
    }

    private boolean isClosed(Ticket ticket) {
        Integer sid = ticket.getStatus() != null ? ticket.getStatus().getId() : null;
        return sid != null
                && (sid.equals(TicketStatus.RESOLVED_ID) || sid.equals(TicketStatus.CANCELLED_ID));
    }

    /** Yorum operasyonlarinda firlatilan domain exception. */
    public static class ReplyException extends RuntimeException {
        public ReplyException(String message) { super(message); }
    }
}
