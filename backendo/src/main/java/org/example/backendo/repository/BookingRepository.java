package org.example.backendo.repository;

import org.example.backendo.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByRoomId(Long roomId);

    List<Booking> findByStato(String stato);

    // Blocca le date per evitare overbooking considerando sia prenotazioni CONFERMATA che IN_ATTESA
    @Query("select b from Booking b where b.room.id = :roomId and b.stato in ('CONFERMATA', 'IN_ATTESA') and b.checkIn < :checkOut and b.checkOut > :checkIn")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId,
                                         @Param("checkIn") LocalDate checkIn,
                                         @Param("checkOut") LocalDate checkOut);

    // Recupera le prenotazioni in un determinato stato create prima del cutoff specificato
    List<Booking> findByStatoAndCreatedAtBefore(String stato, LocalDateTime cutoffTime);
}
