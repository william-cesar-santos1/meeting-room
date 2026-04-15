package br.com.ada.classes.meetingroom.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
public class Room extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservations = new ArrayList<>();

    @Formula("(SELECT COALESCE(v.horas_utilizadas, 0) FROM vw_room_occupancy v WHERE v.room_id = id)")
    private double hoursUsed;

    @Formula("(SELECT COALESCE(v.horas_reservadas, 0) FROM vw_room_occupancy v WHERE v.room_id = id)")
    private double hoursReserved;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public double getHoursUsed() {
        return hoursUsed;
    }

    public double getHoursReserved() {
        return hoursReserved;
    }

}
