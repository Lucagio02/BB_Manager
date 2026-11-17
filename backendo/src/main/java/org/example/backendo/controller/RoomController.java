package org.example.backendo.controller;

import jakarta.validation.Valid;
import org.example.backendo.dto.BookingPriceCalculationRequest;
import org.example.backendo.dto.BookingPriceCalculationResponse;
import org.example.backendo.dto.DailyRateResponse;
import org.example.backendo.dto.RoomResponse;
import org.example.backendo.model.Room;
import org.example.backendo.model.RoomDailyRate;
import org.example.backendo.service.BookingService;
import org.example.backendo.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final BookingService bookingService;

    public RoomController(RoomService roomService, BookingService bookingService) {
        this.roomService = roomService;
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms(
            @RequestParam(name = "onlyAvailable", required = false, defaultValue = "false") boolean onlyAvailable) {
        List<Room> rooms = onlyAvailable ? roomService.getAvailableRooms() : roomService.getAllRooms();
        List<RoomResponse> response = rooms.stream()
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id) {
        Room room = roomService.getRoomById(id)
                .orElseThrow(() -> new RuntimeException("Stanza non trovata con ID: " + id));
        return ResponseEntity.ok(mapToRoomResponse(room));
    }

    @PostMapping("/calculate-price")
    public ResponseEntity<BookingPriceCalculationResponse> calculatePrice(
            @Valid @RequestBody BookingPriceCalculationRequest request) {
        Room room = roomService.getRoomById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Stanza non trovata con ID: " + request.getRoomId()));

        Double price = bookingService.calculateBookingPrice(
                room,
                request.getCheckIn(),
                request.getCheckOut(),
                request.getNumeroOspiti()
        );

        long nights = ChronoUnit.DAYS.between(request.getCheckIn(), request.getCheckOut());

        BookingPriceCalculationResponse response = BookingPriceCalculationResponse.builder()
                .roomId(room.getId())
                .roomName(room.getNomeStanza())
                .checkIn(request.getCheckIn())
                .checkOut(request.getCheckOut())
                .numeroOspiti(request.getNumeroOspiti())
                .numeroNotti(nights)
                .prezzoTotale(price)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/daily-rates")
    public ResponseEntity<List<DailyRateResponse>> getDailyRates(
            @PathVariable Long id,
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<RoomDailyRate> rates = roomService.getDailyRates(id, startDate, endDate);
        List<DailyRateResponse> response = rates.stream()
                .map(r -> DailyRateResponse.builder()
                        .id(r.getId())
                        .roomId(r.getRoom().getId())
                        .data(r.getData())
                        .prezzo(r.getPrezzo())
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private RoomResponse mapToRoomResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .nomeStanza(room.getNomeStanza())
                .postiLetto(room.getPostiLetto())
                .prezzoPerNotte(room.getPrezzoPerNotte())
                .descrizione(room.getDescrizione())
                .disponibile(room.getDisponibile())
                .build();
    }
}
