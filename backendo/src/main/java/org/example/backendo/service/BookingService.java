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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {

    public static final long HOLD_DURATION_MINUTES = 10;

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PricingConfigRepository pricingConfigRepository;
    private final RoomDailyRateRepository roomDailyRateRepository;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          RoomRepository roomRepository,
                          PricingConfigRepository pricingConfigRepository,
                          RoomDailyRateRepository roomDailyRateRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.pricingConfigRepository = pricingConfigRepository;
        this.roomDailyRateRepository = roomDailyRateRepository;
    }

    /**
     * Calcola il prezzo totale del soggiorno scorrendo ogni notte tra check-in e check-out.
     * Per ogni notte verifica se è presente una tariffa personalizzata in RoomDailyRate;
     * in caso contrario applica la tariffa base della stanza (room.getPrezzoPerNotte()).
     * Aggiunge inoltre l'eventuale supplemento per ospiti extra (> 2) per ogni notte.
     */
    public Double calculateBookingPrice(Room room, LocalDate checkIn, LocalDate checkOut, Integer numeroOspiti) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Le date di check-in e check-out non possono essere null");
        }
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new IllegalArgumentException("La data di check-out deve essere successiva al check-in");
        }

        // Recupero supplemento ospiti extra da PricingConfig (default 20.0 se non impostato)
        PricingConfig config = pricingConfigRepository.findById(1L).orElse(null);
        double supplementoExtraPerOspite = (config != null && config.getSupplementoOspiteExtra() != null)
                ? config.getSupplementoOspiteExtra()
                : 20.0;

        int ospitiExtra = (numeroOspiti != null && numeroOspiti > 2) ? (numeroOspiti - 2) : 0;
        double supplementoOspitiPerNotte = ospitiExtra * supplementoExtraPerOspite;

        // Recupero in batch delle tariffe personalizzate per tutte le notti comprese tra checkIn e checkOut - 1
        LocalDate lastNight = checkOut.minusDays(1);
        List<RoomDailyRate> customRates = roomDailyRateRepository.findByRoomIdAndDataBetween(room.getId(), checkIn, lastNight);
        Map<LocalDate, Double> customRateMap = customRates.stream()
                .collect(Collectors.toMap(RoomDailyRate::getData, RoomDailyRate::getPrezzo));

        double totalPrice = 0.0;
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            double tariffaNotte = customRateMap.getOrDefault(date, room.getPrezzoPerNotte());
            totalPrice += (tariffaNotte + supplementoOspitiPerNotte);
        }

        return Math.round(totalPrice * 100.0) / 100.0;
    }

    /**
     * Avvia una prenotazione creando un blocco temporaneo di 10 minuti con stato "IN_ATTESA".
     * Previene l'overbooking controllando la presenza di prenotazioni sia CONFERMATE che IN_ATTESA.
     */
    @Transactional
    public Booking createBooking(Long userId, Long roomId, LocalDate checkIn, LocalDate checkOut, Integer numeroOspiti) {
        // Controllo coerenza date
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Le date di check-in e check-out sono obbligatorie");
        }
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new IllegalArgumentException("La data di check-out deve essere successiva al check-in");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La data di check-in non può essere nel passato");
        }

        // Verifica esistenza di utente e stanza
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + userId));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Stanza non trovata con ID: " + roomId));

        // Controllo capienza ospiti
        if (numeroOspiti == null || numeroOspiti <= 0) {
            throw new IllegalArgumentException("Il numero di ospiti deve essere maggiore di zero");
        }
        if (numeroOspiti > room.getPostiLetto()) {
            throw new IllegalArgumentException("Il numero di ospiti (" + numeroOspiti +
                    ") supera i posti letto disponibili per questa stanza (" + room.getPostiLetto() + ")");
        }

        // Controllo overbooking (blocca se presente prenotazione CONFERMATA o IN_ATTESA valida)
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(roomId, checkIn, checkOut);
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("La stanza selezionata è già occupata o bloccata in attesa di conferma nel periodo richiesto");
        }

        // Calcolo dinamico del totale per notte
        Double totalPrice = calculateBookingPrice(room, checkIn, checkOut, numeroOspiti);

        LocalDateTime now = LocalDateTime.now();

        // Creazione record con blocco temporaneo: stato "IN_ATTESA" e timestamp di creazione
        Booking booking = Booking.builder()
                .user(user)
                .room(room)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .numeroOspiti(numeroOspiti)
                .prezzoTotale(totalPrice)
                .stato(Booking.STATO_IN_ATTESA)
                .createdAt(now)
                .build();

        return bookingRepository.save(booking);
    }

    /**
     * Conferma una prenotazione in stato "IN_ATTESA", verificando che non siano trascorsi più di 10 minuti.
     */
    @Transactional
    public Booking confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata con ID: " + bookingId));

        if (!Booking.STATO_IN_ATTESA.equals(booking.getStato())) {
            throw new IllegalStateException("Impossibile confermare: la prenotazione non è in attesa (stato attuale: " + booking.getStato() + ")");
        }

        // Controllo scadenza 10 minuti
        if (Duration.between(booking.getCreatedAt(), LocalDateTime.now()).toMinutes() >= HOLD_DURATION_MINUTES) {
            booking.setStato(Booking.STATO_SCADUTA);
            bookingRepository.save(booking);
            throw new IllegalStateException("Il tempo limite di 10 minuti per confermare la prenotazione è scaduto");
        }

        booking.setStato(Booking.STATO_CONFERMATA);
        return bookingRepository.save(booking);
    }

    /**
     * Cancella una prenotazione impostando lo stato su "CANCELLATA".
     */
    @Transactional
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata con ID: " + bookingId));

        booking.setStato(Booking.STATO_CANCELLATA);
        return bookingRepository.save(booking);
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<Booking> getBookingsByRoom(Long roomId) {
        return bookingRepository.findByRoomId(roomId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
}
