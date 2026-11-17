package org.example.backendo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.backendo.dto.BookingCreateRequest;
import org.example.backendo.model.Booking;
import org.example.backendo.model.Room;
import org.example.backendo.model.User;
import org.example.backendo.security.CustomUserDetails;
import org.example.backendo.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private ObjectMapper objectMapper;

    private User sampleUser;
    private Room sampleRoom;
    private Authentication guestAuth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleUser = User.builder()
                .id(1L)
                .Nome("Mario")
                .Cognome("Rossi")
                .email("mario@example.com")
                .password("encoded")
                .role_guest("ROLE_GUEST")
                .build();

        sampleRoom = Room.builder()
                .id(10L)
                .nomeStanza("Suite Deluxe")
                .postiLetto(3)
                .prezzoPerNotte(110.0)
                .Disponibile(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(sampleUser);
        guestAuth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void createBooking_createsBookingInPendingStatus() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(2);

        BookingCreateRequest request = BookingCreateRequest.builder()
                .roomId(10L)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .numeroOspiti(2)
                .build();

        Booking pendingBooking = Booking.builder()
                .id(50L)
                .user(sampleUser)
                .room(sampleRoom)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .numeroOspiti(2)
                .prezzoTotale(220.0)
                .stato(Booking.STATO_IN_ATTESA)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingService.createBooking(1L, 10L, checkIn, checkOut, 2))
                .thenReturn(pendingBooking);

        mockMvc.perform(post("/api/bookings")
                        .principal(guestAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.stato").value(Booking.STATO_IN_ATTESA))
                .andExpect(jsonPath("$.prezzoTotale").value(220.0))
                .andExpect(jsonPath("$.roomName").value("Suite Deluxe"));
    }

    @Test
    void confirmBooking_confirmsPendingBooking() throws Exception {
        Booking pendingBooking = Booking.builder()
                .id(50L)
                .user(sampleUser)
                .room(sampleRoom)
                .checkIn(LocalDate.now().plusDays(5))
                .checkOut(LocalDate.now().plusDays(7))
                .numeroOspiti(2)
                .prezzoTotale(220.0)
                .stato(Booking.STATO_IN_ATTESA)
                .createdAt(LocalDateTime.now())
                .build();

        Booking confirmedBooking = Booking.builder()
                .id(50L)
                .user(sampleUser)
                .room(sampleRoom)
                .checkIn(pendingBooking.getCheckIn())
                .checkOut(pendingBooking.getCheckOut())
                .numeroOspiti(2)
                .prezzoTotale(220.0)
                .stato(Booking.STATO_CONFERMATA)
                .createdAt(pendingBooking.getCreatedAt())
                .build();

        when(bookingService.getBookingById(50L)).thenReturn(Optional.of(pendingBooking));
        when(bookingService.confirmBooking(50L)).thenReturn(confirmedBooking);

        mockMvc.perform(put("/api/bookings/50/confirm")
                        .principal(guestAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.stato").value(Booking.STATO_CONFERMATA));
    }

    @Test
    void getMyBookings_returnsUserBookings() throws Exception {
        Booking booking = Booking.builder()
                .id(50L)
                .user(sampleUser)
                .room(sampleRoom)
                .checkIn(LocalDate.now().plusDays(5))
                .checkOut(LocalDate.now().plusDays(7))
                .numeroOspiti(2)
                .prezzoTotale(220.0)
                .stato(Booking.STATO_CONFERMATA)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingService.getBookingsByUser(1L)).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/bookings/my")
                        .principal(guestAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].userEmail").value("mario@example.com"));
    }
}
