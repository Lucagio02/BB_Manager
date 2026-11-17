package org.example.backendo.repository;

import org.example.backendo.model.RoomDailyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomDailyRateRepository extends JpaRepository<RoomDailyRate, Long> {

    Optional<RoomDailyRate> findByRoomIdAndData(Long roomId, LocalDate data);

    List<RoomDailyRate> findByRoomIdAndDataBetween(Long roomId, LocalDate startDate, LocalDate endDate);
}
