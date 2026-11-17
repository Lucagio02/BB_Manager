package org.example.backendo.scheduler;

import org.example.backendo.model.Booking;
import org.example.backendo.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingExpirationSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingExpirationScheduler scheduler;

    @Test
    void releaseExpiredPendingBookings_releasesExpiredBookings() {
        Booking booking1 = Booking.builder()
                .id(1L)
                .stato(Booking.STATO_IN_ATTESA)
                .createdAt(LocalDateTime.now().minusMinutes(15))
                .build();

        Booking booking2 = Booking.builder()
                .id(2L)
                .stato(Booking.STATO_IN_ATTESA)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .build();

        when(bookingRepository.findByStatoAndCreatedAtBefore(eq(Booking.STATO_IN_ATTESA), any(LocalDateTime.class)))
                .thenReturn(List.of(booking1, booking2));

        int releasedCount = scheduler.releaseExpiredPendingBookings();

        assertEquals(2, releasedCount);
        assertEquals(Booking.STATO_SCADUTA, booking1.getStato());
        assertEquals(Booking.STATO_SCADUTA, booking2.getStato());
        verify(bookingRepository, times(1)).saveAll(List.of(booking1, booking2));
    }

    @Test
    void releaseExpiredPendingBookings_nothingToRelease() {
        when(bookingRepository.findByStatoAndCreatedAtBefore(eq(Booking.STATO_IN_ATTESA), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        int releasedCount = scheduler.releaseExpiredPendingBookings();

        assertEquals(0, releasedCount);
        verify(bookingRepository, never()).saveAll(any());
    }
}
