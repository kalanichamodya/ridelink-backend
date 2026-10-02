package com.ridelink.fare;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ChargeRepository extends JpaRepository<Charge,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Charge c where c.rideId=:id")
    Optional<Charge> locked(@Param("id") UUID id);
}
