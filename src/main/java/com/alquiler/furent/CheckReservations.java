package com.alquiler.furent;

import com.alquiler.furent.model.Reservation;
import com.alquiler.furent.repository.ReservationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CheckReservations implements CommandLineRunner {
    private final ReservationRepository reservationRepository;

    public CheckReservations(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== CHECKING RESERVATIONS START ===");
        List<Reservation> all = reservationRepository.findAll();
        System.out.println("Total reservations: " + all.size());
        for (Reservation r : all) {
            System.out.println("ID: " + r.getId() + ", Estado: " + r.getEstado() + ", FechaInicio: " + r.getFechaInicio() + 
                               ", Total: " + r.getTotal() + ", Items: " + (r.getItems() != null ? r.getItems().size() : 0));
        }
        System.out.println("=== CHECKING RESERVATIONS END ===");
    }
}
