package org.example.backendo.service;

import org.example.backendo.model.Room;
import org.example.backendo.model.RoomDailyRate;
import org.example.backendo.repository.RoomDailyRateRepository;
import org.example.backendo.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomDailyRateRepository roomDailyRateRepository;

    public RoomService(RoomRepository roomRepository, RoomDailyRateRepository roomDailyRateRepository) {
        this.roomRepository = roomRepository;
        this.roomDailyRateRepository = roomDailyRateRepository;
    }

    public Room addRoom(Room room) {
        // Regola di business: impedisce l'inserimento di stanze omonime
        Optional<Room> existingRoom = roomRepository.findByNomeStanza(room.getNomeStanza());
        if (existingRoom.isPresent()) {
            throw new RuntimeException("Esiste già una stanza chiamata: " + room.getNomeStanza());
        }
        if (room.getDisponibile() == null) {
            room.setDisponibile(true);
        }
        return roomRepository.save(room);
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getAvailableRooms() {
        return roomRepository.findByDisponibileTrue();
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    /**
     * Imposta o aggiorna la tariffa personalizzata per una stanza in un singolo giorno.
     */
    @Transactional
    public RoomDailyRate setDailyRate(Long roomId, LocalDate data, Double prezzo) {
        if (data == null) {
            throw new IllegalArgumentException("La data non può essere null");
        }
        if (prezzo == null || prezzo <= 0) {
            throw new IllegalArgumentException("Il prezzo per notte deve essere maggiore di zero");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Stanza non trovata con ID: " + roomId));

        RoomDailyRate dailyRate = roomDailyRateRepository.findByRoomIdAndData(roomId, data)
                .orElseGet(() -> RoomDailyRate.builder()
                        .room(room)
                        .data(data)
                        .build());

        dailyRate.setPrezzo(prezzo);
        return roomDailyRateRepository.save(dailyRate);
    }

    /**
     * Imposta o aggiorna la tariffa personalizzata per una stanza in un intervallo di date (estremi inclusi).
     */
    @Transactional
    public List<RoomDailyRate> setDailyRatesForPeriod(Long roomId, LocalDate startDate, LocalDate endDate, Double prezzo) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Le date di inizio e fine non possono essere null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La data di inizio non può essere successiva alla data di fine");
        }
        if (prezzo == null || prezzo <= 0) {
            throw new IllegalArgumentException("Il prezzo per notte deve essere maggiore di zero");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Stanza non trovata con ID: " + roomId));

        // Carica le tariffe già esistenti nel periodo per aggiornarle senza duplicare
        List<RoomDailyRate> existingRates = roomDailyRateRepository.findByRoomIdAndDataBetween(roomId, startDate, endDate);
        Map<LocalDate, RoomDailyRate> existingMap = existingRates.stream()
                .collect(Collectors.toMap(RoomDailyRate::getData, Function.identity()));

        List<RoomDailyRate> ratesToSave = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            RoomDailyRate dailyRate = existingMap.get(date);
            if (dailyRate != null) {
                dailyRate.setPrezzo(prezzo);
                ratesToSave.add(dailyRate);
            } else {
                ratesToSave.add(RoomDailyRate.builder()
                        .room(room)
                        .data(date)
                        .prezzo(prezzo)
                        .build());
            }
        }

        return roomDailyRateRepository.saveAll(ratesToSave);
    }

    /**
     * Recupera le tariffe giornaliere personalizzate per una stanza in un intervallo di date.
     */
    public List<RoomDailyRate> getDailyRates(Long roomId, LocalDate startDate, LocalDate endDate) {
        return roomDailyRateRepository.findByRoomIdAndDataBetween(roomId, startDate, endDate);
    }

    /**
     * Recupera la tariffa personalizzata per una stanza in un giorno specifico.
     */
    public Optional<RoomDailyRate> getDailyRate(Long roomId, LocalDate data) {
        return roomDailyRateRepository.findByRoomIdAndData(roomId, data);
    }

    /**
     * Aggiorna i dati anagrafici di una stanza.
     */
    @Transactional
    public Room updateRoom(Long id, Room updatedRoom) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stanza non trovata con ID: " + id));

        // Se cambia il nome stanza, verifica che non sia già in uso da un'altra stanza
        if (!room.getNomeStanza().equalsIgnoreCase(updatedRoom.getNomeStanza())) {
            Optional<Room> existing = roomRepository.findByNomeStanza(updatedRoom.getNomeStanza());
            if (existing.isPresent() && !existing.get().getId().equals(id)) {
                throw new RuntimeException("Esiste già un'altra stanza chiamata: " + updatedRoom.getNomeStanza());
            }
        }

        room.setNomeStanza(updatedRoom.getNomeStanza());
        room.setPostiLetto(updatedRoom.getPostiLetto());
        room.setPrezzoPerNotte(updatedRoom.getPrezzoPerNotte());
        room.setDescrizione(updatedRoom.getDescrizione());
        if (updatedRoom.getDisponibile() != null) {
            room.setDisponibile(updatedRoom.getDisponibile());
        }

        return roomRepository.save(room);
    }

    /**
     * Elimina una stanza per ID.
     */
    @Transactional
    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new RuntimeException("Stanza non trovata con ID: " + id);
        }
        roomRepository.deleteById(id);
    }
}
