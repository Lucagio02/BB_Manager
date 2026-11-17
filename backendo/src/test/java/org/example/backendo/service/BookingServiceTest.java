package org.example.backendo.service;

import org.example.backendo.model.Booking;
import org.example.backendo.model.PricingConfig;
import org.example.backendo.model.Room;
import org.example.backendo.model.RoomDailyRate;
import org.example.backendo.model.User;
import org.example.backendo.repository.BookingRepository;
import org.example.backendo.repository.PricingConfigRepository;
import org.example.backendo.repository.RoomDailyRateRepository;
import org.example.backendo.repository.RoomRepository;
import org.example.backendo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private PricingConfigRepository pricingConfigRepository;

    @Mock
    private RoomDailyRateRepository roomDailyRateRepository;

    @InjectMocks
    private BookingService bookingService;

    private User sampleUser;
    private Room sampleRoom;
    private PricingConfig sampleConfig;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .Nome("Mario")
                .Cognome("Rossi")
                .email("mario@example.com")
                .password("secret")
                .role_guest("ROLE_GUEST")
                .build();

        sampleRoom = Room.builder()
                .id(10L)
                .nomeStanza("Suite Vista Mare")
                .postiLetto(4)
                .prezzoPerNotte(100.0)
                .Disponibile(true)
                .build();

        sampleConfig = PricingConfig.builder()
                .id(1L)
                .supplementoOspiteExtra(25.0)
                .build();
    }

    @Test
    void calculateBookingPrice_withDefaultRatesAndNoExtraGuests() {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        LocalDate checkOut = checkIn.plusDays(3); // 3 notti

        when(pricingConfigRepository.findById(1L)).thenReturn(Optional.of(sampleConfig));
        when(roomDailyRateRepository.findByRoomIdAndDataBetween(10L, checkIn, checkOut.minusDays(1)))
                .thenReturn(Collections.emptyList());

        Double price = bookingService.calculateBookingPrice(sampleRoom, checkIn, checkOut, 2);

        // 3 notti * 100.0 = 300.0, 0 supplemento
        assertEquals(300.0, price);
    }

    @Test
    void calculateBookingPrice_withExtraGuests() {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(2); // 2 notti
        int ospiti = 4; // 2 ospiti extra -> 2 * 25.0 = 50.0 a notte

        when(pricingConfigRepository.findById(1L)).thenReturn(Optional.of(sampleConfig));
        when(roomDailyRateRepository.findByRoomIdAndDataBetween(10L, checkIn, checkOut.minusDays(1)))
                .thenReturn(Collections.emptyList());

        Double price = bookingService.calculateBookingPrice(sampleRoom, checkIn, checkOut, ospiti);

        // Notte: 100.0 + 50.0 = 150.0 a notte * 2 notti = 300.0
        assertEquals(300.0, price);
    }

    @Test
    void calculateBookingPrice_withCustomDailyRates() {
        LocalDate checkIn = LocalDate.now().plusDays(20);
        LocalDate checkOut = checkIn.plusDays(3); // 3 notti: checkIn, checkIn+1, checkIn+2

        List<RoomDailyRate> customRates = List.of(
                RoomDailyRate.builder().id(1L).room(sampleRoom).data(checkIn).prezzo(150.0).build(),
                RoomDailyRate.builder().id(2L).room(sampleRoom).data(checkIn.plusDays(1)).prezzo(180.0).build()
        );

        when(pricingConfigRepository.findById(1L)).thenReturn(Optional.of(sampleConfig));
        when(roomDailyRateRepository.findByRoomIdAndDataBetween(10L, checkIn, checkOut.minusDays(1)))
                .thenReturn(customRates);

        Double price = bookingService.calculateBookingPrice(sampleRoom, checkIn, checkOut, 2);

        // Notte 1 (150.0) + Notte 2 (180.0) + Notte 3 (100.0 default) = 430.0
        assertEquals(430.0, price);
    }

    @Test
    void createBooking_createsWithStatusInAttesaAndCreatedAt() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = checkIn.plusDays(2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(roomRepository.findById(10L)).thenReturn(Optional.of(sampleRoom));
        when(bookingRepository.findOverlappingBookings(10L, checkIn, checkOut)).thenReturn(Collections.emptyList());
        when(pricingConfigRepository.findById(1L)).thenReturn(Optional.of(sampleConfig));
        when(roomDailyRateRepository.findByRoomIdAndDataBetween(10L, checkIn, checkOut.minusDays(1)))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(1L, 10L, checkIn, checkOut, 3);

        assertNotNull(booking);
        assertEquals(sampleUser, booking.getUser());
        assertEquals(sampleRoom, booking.getRoom());
        assertEquals(3, booking.getNumeroOspiti());
        assertEquals(Booking.STATO_IN_ATTESA, booking.getStato());
        assertNotNull(booking.getCreatedAt());
        // 2 notti * (100 base + 25 extra) = 250.0
        assertEquals(250.0, booking.getPrezzoTotale());
    }

    @Test
    void confirmBooking_successfulWithinHoldDuration() {
        Booking pendingBooking = Booking.builder()
                .id(100L)
                .stato(Booking.STATO_IN_ATTESA)
                .createdAt(LocalDateTime.now().minusMinutes(5)) // 5 min fa, entro i 10 min
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking confirmed = bookingService.confirmBooking(100L);

        assertEquals(Booking.STATO_CONFERMATA, confirmed.getStato());
    }

    @Test
    void confirmBooking_throwsWhenExpired() {
        Booking expiredBooking = Booking.builder()
                .id(101L)
                .stato(Booking.STATO_IN_ATTESA)
                .createdAt(LocalDateTime.now().minusMinutes(12)) // 12 min fa, scaduta!
                .build();

        when(bookingRepository.findById(101L)).thenReturn(Optional.of(expiredBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bookingService.confirmBooking(101L));

        assertTrue(ex.getMessage().contains("scaduto"));
        assertEquals(Booking.STATO_SCADUTA, expiredBooking.getStato());
    }

    @Test
    void cancelBooking_successful() {
        Booking booking = Booking.builder()
                .id(102L)
                .stato(Booking.STATO_IN_ATTESA)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findById(102L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        Booking cancelled = bookingService.cancelBooking(102L);

        assertEquals(Booking.STATO_CANCELLATA, cancelled.getStato());
    }
}
