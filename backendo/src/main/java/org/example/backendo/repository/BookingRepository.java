package org.example.backendo.repository;

import org.example.backendo.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
@Repository
public interface BookingRepository  extends JpaRepository<Booking,Long>{

    List<Booking> findByUserId(Long userId);

    List<Booking> findByRoomId(Long roomId);

    // Query esplicita in JPQL per verificare la disponibilità (prevenire overbooking)
    @Query("select b from Booking b where b.room.id = :roomId and b.stato = 'CONFERMATA' and b.checkIn < :checkOut and b.checkOut > :checkIn")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId,
                                          @Param("checkIn") LocalDate checkIn,
                                          @Param("checkOut") LocalDate checkOut);
}
