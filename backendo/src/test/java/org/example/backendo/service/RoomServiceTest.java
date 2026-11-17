package org.example.backendo.service;

import org.example.backendo.model.Room;
import org.example.backendo.model.RoomDailyRate;
import org.example.backendo.repository.RoomDailyRateRepository;
import org.example.backendo.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomDailyRateRepository roomDailyRateRepository;

    @InjectMocks
    private RoomService roomService;

    private Room sampleRoom;

    @BeforeEach
    void setUp() {
        sampleRoom = Room.builder()
                .id(1L)
                .nomeStanza("Camera Deluxe")
                .postiLetto(2)
                .prezzoPerNotte(80.0)
                .Disponibile(true)
                .build();
    }

    @Test
    void setDailyRate_newRate() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomDailyRateRepository.findByRoomIdAndData(1L, date)).thenReturn(Optional.empty());
        when(roomDailyRateRepository.save(any(RoomDailyRate.class))).thenAnswer(i -> i.getArgument(0));

        RoomDailyRate saved = roomService.setDailyRate(1L, date, 150.0);

        assertNotNull(saved);
        assertEquals(date, saved.getData());
        assertEquals(150.0, saved.getPrezzo());
        assertEquals(sampleRoom, saved.getRoom());
    }

    @Test
    void setDailyRate_updateExistingRate() {
        LocalDate date = LocalDate.of(2026, 8, 15);
        RoomDailyRate existing = RoomDailyRate.builder()
                .id(100L)
                .room(sampleRoom)
                .data(date)
                .prezzo(120.0)
                .build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomDailyRateRepository.findByRoomIdAndData(1L, date)).thenReturn(Optional.of(existing));
        when(roomDailyRateRepository.save(any(RoomDailyRate.class))).thenAnswer(i -> i.getArgument(0));

        RoomDailyRate updated = roomService.setDailyRate(1L, date, 160.0);

        assertEquals(160.0, updated.getPrezzo());
        assertEquals(100L, updated.getId());
    }

    @Test
    void setDailyRatesForPeriod_success() {
        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 3); // 3 giorni: 1, 2, 3 luglio

        when(roomRepository.findById(1L)).thenReturn(Optional.of(sampleRoom));
        when(roomDailyRateRepository.findByRoomIdAndDataBetween(1L, start, end)).thenReturn(Collections.emptyList());
        when(roomDailyRateRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<RoomDailyRate> results = roomService.setDailyRatesForPeriod(1L, start, end, 130.0);

        assertEquals(3, results.size());
        assertTrue(results.stream().allMatch(r -> r.getPrezzo().equals(130.0)));
        assertEquals(start, results.get(0).getData());
        assertEquals(end, results.get(2).getData());
    }
}
