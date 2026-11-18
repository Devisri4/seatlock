package com.devisri.seatlock.booking;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    List<Booking> findByGuestIdOrderByCreatedAtDescIdDesc(String guestId);
}
