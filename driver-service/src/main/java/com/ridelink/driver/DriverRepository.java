package com.ridelink.driver;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface DriverRepository extends JpaRepository<Driver,UUID> {
    List<Driver> findByAvailableTrueAndReservedRideIsNullAndServiceAreaOrderByIdAsc(String serviceArea);
    Optional<Driver> findByReservedRide(UUID rideId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from Driver d where d.id=:id")
    Optional<Driver> locked(@Param("id") UUID id);
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    @Query("update Driver d set d.reservedRide=:rideId, d.available=false, d.version=d.version+1 where d.id=:id and d.available=true and d.reservedRide is null")
    int claim(@Param("id") UUID id, @Param("rideId") UUID rideId);
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    @Query("update Driver d set d.reservedRide=null, d.available=true, d.version=d.version+1 where d.reservedRide=:rideId")
    int releaseReservation(@Param("rideId") UUID rideId);
}
