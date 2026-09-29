package org.example.backendo.repository;

import org.example.backendo.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository       //grazie a questa annotazione specifichiamo che la classe si occupa di archiviare, recuperare o cercare dei dati

public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByNomeStanza(String nomeStanza);

    List<Room> findByDisponibileTrue();

}
