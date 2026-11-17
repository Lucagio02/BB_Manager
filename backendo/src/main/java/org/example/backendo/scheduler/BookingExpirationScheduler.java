package org.example.backendo.scheduler;

import org.example.backendo.model.Booking;
import org.example.backendo.repository.BookingRepository;
import org.example.backendo.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(BookingExpirationScheduler.class);

    private final BookingRepository bookingRepository;

    public BookingExpirationScheduler(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * Esegue ogni minuto il controllo delle prenotazioni in stato "IN_ATTESA".
     * Se una prenotazione è stata creata più di 10 minuti fa, viene impostata a "SCADUTA",
     * liberando automaticamente la stanza per nuove prenotazioni.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public int releaseExpiredPendingBookings() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(BookingService.HOLD_DURATION_MINUTES);
        List<Booking> expiredBookings = bookingRepository.findByStatoAndCreatedAtBefore(Booking.STATO_IN_ATTESA, cutoffTime);

        if (!expiredBookings.isEmpty()) {
            for (Booking booking : expiredBookings) {
                booking.setStato(Booking.STATO_SCADUTA);
            }
            bookingRepository.saveAll(expiredBookings);
            log.info("Rilasciate {} prenotazioni scadute (stato aggiornato a '{}')",
                    expiredBookings.size(), Booking.STATO_SCADUTA);
        }

        return expiredBookings.size();
    }
}
