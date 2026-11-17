package org.example.backendo.controller;

import jakarta.validation.Valid;
import org.example.backendo.dto.*;
import org.example.backendo.model.Booking;
import org.example.backendo.model.PricingConfig;
import org.example.backendo.model.Room;
import org.example.backendo.model.RoomDailyRate;
import org.example.backendo.repository.BookingRepository;
import org.example.backendo.service.PricingConfigService;
import org.example.backendo.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final RoomService roomService;
    private final BookingRepository bookingRepository;
    private final PricingConfigService pricingConfigService;

    public AdminController(RoomService roomService,
                           BookingRepository bookingRepository,
                           PricingConfigService pricingConfigService) {
        this.roomService = roomService;
        this.bookingRepository = bookingRepository;
        this.pricingConfigService = pricingConfigService;
    }

    // --- GESTIONE STANZE ---

    @PostMapping("/rooms")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        Room room = Room.builder()
                .nomeStanza(request.getNomeStanza())
                .postiLetto(request.getPostiLetto())
                .prezzoPerNotte(request.getPrezzoPerNotte())
                .descrizione(request.getDescrizione())
                .Disponibile(request.getDisponibile() != null ? request.getDisponibile() : true)
                .build();

        Room saved = roomService.addRoom(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToRoomResponse(saved));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        Room room = Room.builder()
                .nomeStanza(request.getNomeStanza())
                .postiLetto(request.getPostiLetto())
                .prezzoPerNotte(request.getPrezzoPerNotte())
                .descrizione(request.getDescrizione())
                .Disponibile(request.getDisponibile())
                .build();

        Room updated = roomService.updateRoom(id, room);
        return ResponseEntity.ok(mapToRoomResponse(updated));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }

    // --- GESTIONE TARIFFE CALENDARIO ---

    @PostMapping("/rooms/{id}/daily-rate")
    public ResponseEntity<DailyRateResponse> setDailyRate(
            @PathVariable Long id,
            @Valid @RequestBody DailyRateRequest request) {
        RoomDailyRate rate = roomService.setDailyRate(id, request.getData(), request.getPrezzo());
        return ResponseEntity.ok(DailyRateResponse.builder()
                .id(rate.getId())
                .roomId(rate.getRoom().getId())
                .data(rate.getData())
                .prezzo(rate.getPrezzo())
                .build());
    }

    @PostMapping("/rooms/{id}/daily-rates-range")
    public ResponseEntity<List<DailyRateResponse>> setDailyRatesRange(
            @PathVariable Long id,
            @Valid @RequestBody DailyRateRangeRequest request) {
        List<RoomDailyRate> rates = roomService.setDailyRatesForPeriod(
                id,
                request.getStartDate(),
                request.getEndDate(),
                request.getPrezzo()
        );

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

    // --- MONITORAGGIO PRENOTAZIONI ---

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponse>> getAllBookings(
            @RequestParam(name = "stato", required = false) String stato) {
        List<Booking> bookings = (stato != null && !stato.isBlank())
                ? bookingRepository.findByStato(stato.toUpperCase())
                : bookingRepository.findAll();

        List<BookingResponse> response = bookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // --- GESTIONE CONFIGURAZIONE PREZZI GLOBALI ---

    @GetMapping("/pricing-config")
    public ResponseEntity<PricingConfig> getPricingConfig() {
        return ResponseEntity.ok(pricingConfigService.getConfig());
    }

    @PutMapping("/pricing-config")
    public ResponseEntity<PricingConfig> updatePricingConfig(@Valid @RequestBody PricingConfigRequest request) {
        return ResponseEntity.ok(pricingConfigService.updateConfig(request));
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

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .roomId(booking.getRoom().getId())
                .roomName(booking.getRoom().getNomeStanza())
                .userId(booking.getUser().getId())
                .userEmail(booking.getUser().getEmail())
                .userName(booking.getUser().getNome() + " " + booking.getUser().getCognome())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .numeroOspiti(booking.getNumeroOspiti())
                .prezzoTotale(booking.getPrezzoTotale())
                .stato(booking.getStato())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
