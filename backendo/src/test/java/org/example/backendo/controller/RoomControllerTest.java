package org.example.backendo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.backendo.dto.BookingPriceCalculationRequest;
import org.example.backendo.model.Room;
import org.example.backendo.model.RoomDailyRate;
import org.example.backendo.service.BookingService;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoomService roomService;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private RoomController roomController;

    private ObjectMapper objectMapper;

    private Room sampleRoom;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleRoom = Room.builder()
                .id(1L)
                .nomeStanza("Camera Deluxe")
                .postiLetto(3)
                .prezzoPerNotte(90.0)
                .descrizione("Splendida camera con vista panoramica")
                .Disponibile(true)
                .build();
    }

    @Test
    void getAllRooms_returnsRoomList() throws Exception {
        when(roomService.getAllRooms()).thenReturn(List.of(sampleRoom));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nomeStanza").value("Camera Deluxe"))
                .andExpect(jsonPath("$[0].prezzoPerNotte").value(90.0));
    }

    @Test
    void calculatePrice_returnsQuotation() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(3);

        BookingPriceCalculationRequest request = BookingPriceCalculationRequest.builder()
                .roomId(1L)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .numeroOspiti(3)
                .build();

        when(roomService.getRoomById(1L)).thenReturn(Optional.of(sampleRoom));
        when(bookingService.calculateBookingPrice(sampleRoom, checkIn, checkOut, 3))
                .thenReturn(330.0); // 3 notti * (90 + 20 extra) = 330.0

        mockMvc.perform(post("/api/rooms/calculate-price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1))
                .andExpect(jsonPath("$.roomName").value("Camera Deluxe"))
                .andExpect(jsonPath("$.numeroNotti").value(3))
                .andExpect(jsonPath("$.prezzoTotale").value(330.0));
    }

    @Test
    void getDailyRates_returnsCalendarRates() throws Exception {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 2);

        RoomDailyRate rate = RoomDailyRate.builder()
                .id(10L)
                .room(sampleRoom)
                .data(start)
                .prezzo(140.0)
                .build();

        when(roomService.getDailyRates(1L, start, end)).thenReturn(List.of(rate));

        mockMvc.perform(get("/api/rooms/1/daily-rates")
                        .param("start", "2026-08-01")
                        .param("end", "2026-08-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomId").value(1))
                .andExpect(jsonPath("$[0].data").value("2026-08-01"))
                .andExpect(jsonPath("$[0].prezzo").value(140.0));
    }
}
