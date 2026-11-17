package org.example.backendo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.backendo.dto.DailyRateRangeRequest;
import org.example.backendo.dto.PricingConfigRequest;
import org.example.backendo.dto.RoomRequest;
import org.example.backendo.model.Booking;
import org.example.backendo.model.PricingConfig;
import org.example.backendo.model.Room;
import org.example.backendo.model.RoomDailyRate;
import org.example.backendo.model.User;
import org.example.backendo.repository.BookingRepository;
import org.example.backendo.service.PricingConfigService;
import org.example.backendo.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoomService roomService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PricingConfigService pricingConfigService;

    @InjectMocks
    private AdminController adminController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createRoom_success() throws Exception {
        RoomRequest request = RoomRequest.builder()
                .nomeStanza("Camera Vista Mare")
                .postiLetto(3)
                .prezzoPerNotte(120.0)
                .descrizione("Camera con terrazzo")
                .disponibile(true)
                .build();

        Room savedRoom = Room.builder()
                .id(1L)
                .nomeStanza("Camera Vista Mare")
                .postiLetto(3)
                .prezzoPerNotte(120.0)
                .descrizione("Camera con terrazzo")
                .Disponibile(true)
                .build();

        when(roomService.addRoom(any(Room.class))).thenReturn(savedRoom);

        mockMvc.perform(post("/api/admin/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nomeStanza").value("Camera Vista Mare"))
                .andExpect(jsonPath("$.prezzoPerNotte").value(120.0));
    }

    @Test
    void setDailyRatesRange_success() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 10);
        LocalDate end = LocalDate.of(2026, 8, 12);

        DailyRateRangeRequest request = DailyRateRangeRequest.builder()
                .startDate(start)
                .endDate(end)
                .prezzo(150.0)
                .build();

        Room room = Room.builder().id(1L).build();

        List<RoomDailyRate> createdRates = List.of(
                RoomDailyRate.builder().id(101L).room(room).data(start).prezzo(150.0).build(),
                RoomDailyRate.builder().id(102L).room(room).data(start.plusDays(1)).prezzo(150.0).build(),
                RoomDailyRate.builder().id(103L).room(room).data(end).prezzo(150.0).build()
        );

        when(roomService.setDailyRatesForPeriod(eq(1L), eq(start), eq(end), eq(150.0)))
                .thenReturn(createdRates);

        mockMvc.perform(post("/api/admin/rooms/1/daily-rates-range")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].prezzo").value(150.0))
                .andExpect(jsonPath("$[0].data").value("2026-08-10"));
    }

    @Test
    void getAllBookings_withStatusFilter() throws Exception {
        User user = User.builder().id(1L).Nome("Luca").Cognome("Bianchi").email("luca@example.com").build();
        Room room = Room.builder().id(2L).nomeStanza("Suite").build();

        Booking booking = Booking.builder()
                .id(10L)
                .user(user)
                .room(room)
                .checkIn(LocalDate.now().plusDays(1))
                .checkOut(LocalDate.now().plusDays(3))
                .numeroOspiti(2)
                .prezzoTotale(200.0)
                .stato(Booking.STATO_IN_ATTESA)
                .createdAt(LocalDateTime.now())
                .build();

        when(bookingRepository.findByStato("IN_ATTESA")).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/admin/bookings").param("stato", "IN_ATTESA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].stato").value("IN_ATTESA"))
                .andExpect(jsonPath("$[0].userName").value("Luca Bianchi"));
    }

    @Test
    void updatePricingConfig_success() throws Exception {
        PricingConfigRequest request = PricingConfigRequest.builder()
                .supplementoOspiteExtra(30.0)
                .moltiplicatoreAgosto(1.6)
                .build();

        PricingConfig updated = PricingConfig.builder()
                .id(1L)
                .supplementoOspiteExtra(30.0)
                .moltiplicatoreAgosto(1.6)
                .build();

        when(pricingConfigService.updateConfig(any(PricingConfigRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/admin/pricing-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplementoOspiteExtra").value(30.0))
                .andExpect(jsonPath("$.moltiplicatoreAgosto").value(1.6));
    }
}
