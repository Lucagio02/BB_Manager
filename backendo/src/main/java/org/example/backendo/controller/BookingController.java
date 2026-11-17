package org.example.backendo.controller;

import jakarta.validation.Valid;
import org.example.backendo.dto.BookingCreateRequest;
import org.example.backendo.dto.BookingResponse;
import org.example.backendo.model.Booking;
import org.example.backendo.security.CustomUserDetails;
import org.example.backendo.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Booking booking = bookingService.createBooking(
                userDetails.getId(),
                request.getRoomId(),
                request.getCheckIn(),
                request.getCheckOut(),
                request.getNumeroOspiti()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToBookingResponse(booking));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBooking(
            @PathVariable Long id,
            Authentication authentication) {
        checkOwnershipOrAdmin(id, authentication);
        Booking confirmed = bookingService.confirmBooking(id);
        return ResponseEntity.ok(mapToBookingResponse(confirmed));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        checkOwnershipOrAdmin(id, authentication);
        Booking cancelled = bookingService.cancelBooking(id);
        return ResponseEntity.ok(mapToBookingResponse(cancelled));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        List<Booking> bookings = bookingService.getBookingsByUser(userDetails.getId());
        List<BookingResponse> response = bookings.stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        checkOwnershipOrAdmin(id, authentication);
        Booking booking = bookingService.getBookingById(id)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata con ID: " + id));
        return ResponseEntity.ok(mapToBookingResponse(booking));
    }

    private void checkOwnershipOrAdmin(Long bookingId, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            Booking booking = bookingService.getBookingById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Prenotazione non trovata con ID: " + bookingId));
            if (!java.util.Objects.equals(booking.getUser().getId(), userDetails.getId())) {
                throw new AccessDeniedException("Non hai i permessi per accedere a questa prenotazione");
            }
        }
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
