package org.example.backendo.service;
import org.example.backendo.model.Booking;
import org.example.backendo.model.PricingConfig;
import org.example.backendo.model.Room;
import org.example.backendo.model.User;
import org.example.backendo.repository.BookingRepository;
import org.example.backendo.repository.PricingConfigRepository;
import org.example.backendo.repository.RoomRepository;
import org.example.backendo.repository.UserRepository;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.util.List;
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PricingConfigRepository pricingConfigRepository;

    public BookingService(BookingRepository bookingRepository, UserRepository userRepository, RoomRepository roomRepository, PricingConfigRepository pricingConfigRepository) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.pricingConfigRepository = pricingConfigRepository;
    }

    public Booking createBooking(Long userId, Long roomId, LocalDate checkIn, LocalDate checkOut,Integer numeroOspiti){
        //controllo coerenza date
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            throw new RuntimeException("La data di check-out deve essere successiva al check-in");
        }
        //Verifica esistenza di Utente e stanza
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + userId));
        Room room= roomRepository.findById(roomId)
                .orElseThrow(()-> new RuntimeException("Stanza non trovata con Id:"+roomId));

        //Controllo per evitare l'overbooking
        List<Booking> overlapping=bookingRepository.findOverlappingBookings(roomId,checkIn,checkOut);
        if(!overlapping.isEmpty()){
            throw new RuntimeException("La stanza seleziona è gia occupata nel periodo richiesto");

        }

        PricingConfig config=pricingConfigRepository.findById(1L).orElseGet(()->PricingConfig.builder()
                .moltiplicatoreGiugnoSettembre(1.15)
                .moltiplicatoreLuglio(1.30)
                .moltiplicatoreAgosto(1.50)
                .supplementoOspiteExtra(20.0)
                .build());
        double moltiplicatoreStagione=1.0;
        int meseCheckIn=checkIn.getMonthValue();

        if(meseCheckIn==6||meseCheckIn==9){
            moltiplicatoreStagione= config.getMoltiplicatoreGiugnoSettembre();
        } else if(meseCheckIn==7){
            moltiplicatoreStagione=config.getMoltiplicatoreLuglio();
        } else if (meseCheckIn==8) {
            moltiplicatoreStagione= config.getMoltiplicatoreAgosto();
        }
        double prezzoNotteStagionale=room.getPrezzoPerNotte()*moltiplicatoreStagione;

        double supplementoOspiti=0.0;
        if(numeroOspiti>2){
            supplementoOspiti=(numeroOspiti-2)*config.getSupplementoOspiteExtra();
        }
        double totalPrice=prezzoNotteStagionale+supplementoOspiti;

        // Salvataggio della prenotazione confermata
        Booking booking = Booking.builder()
                .user(user)
                .room(room)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .prezzoTotale(totalPrice)
                .stato("CONFERMATA")
                .build();

        return bookingRepository.save(booking);
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
    }

