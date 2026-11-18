package com.devsam.urbantrend.Security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, single-use tickets for opening the live-notification stream.
 *
 * A browser EventSource cannot send an Authorization header, so the stream used to take
 * the access token as a query parameter, which ends up in server and proxy logs. The
 * client now asks for a ticket with its normal bearer token and puts only the ticket in
 * the URL. A ticket works once, for a few seconds, and only on the stream endpoint, so a
 * logged URL gives an attacker nothing.
 *
 * Tickets are held in memory like one-time passwords: they are lost on restart, which at
 * worst makes the client ask for a new one.
 */
@Service
public class StreamTicketService {

    static final long TTL_SECONDS = 30;

    private static final SecureRandom RANDOM = new SecureRandom();

    private record Ticket(String email, Instant expiry) {}

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public String issue(String email) {
        Instant now = Instant.now();
        tickets.values().removeIf(ticket -> ticket.expiry().isBefore(now));

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tickets.put(ticket, new Ticket(email, now.plusSeconds(TTL_SECONDS)));
        return ticket;
    }

    /** Returns the ticket owner's email and burns the ticket; empty if unknown, used or expired. */
    public Optional<String> redeem(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        Ticket found = tickets.remove(ticket);
        if (found == null || found.expiry().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(found.email());
    }
}
