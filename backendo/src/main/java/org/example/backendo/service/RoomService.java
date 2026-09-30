package org.example.backendo.service;

import org.example.backendo.model.Room;
import org.example.backendo.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository){
        this.roomRepository=roomRepository;
    }

    public Room addRoom(Room room){
        //Regola di business: impedisce l'inserimento di stanze omonime
        Optional<Room> existingRoom= roomRepository.findByNomeStanza(room.getNomeStanza());
        if(existingRoom.isPresent()){
            throw new RuntimeException("Esiste già una stanza chiamata"+room.getNomeStanza());

        }
        if(room.getDisponibile()==null){
            room.setDisponibile(true);
        }
        return roomRepository.save(room);
    }
    public List<Room> getAllRooms(){
        return roomRepository.findAll();
    }

    public List<Room> getAvailableRooms(){
        return roomRepository.findByDisponibileTrue();
    }

    public Optional<Room> getRoomById(Long id){
        return roomRepository.findById(id);
    }
}
