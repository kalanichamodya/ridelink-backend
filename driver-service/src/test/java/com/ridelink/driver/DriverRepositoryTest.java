package com.ridelink.driver;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = {"spring.datasource.password=", "ridelink.jwt-secret=unused-test-only-value-at-least-32-characters",
    "ridelink.service-key=unused-test-only-value-at-least-32-characters"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DriverRepositoryTest {
    @Autowired DriverRepository repository;
    @Autowired PlatformTransactionManager manager;

    @Test void concurrentClaimsCannotAssignOneDriverTwice() throws Exception {
        Driver driver = new Driver(); driver.id = UUID.randomUUID(); driver.plate = "TEST-" + UUID.randomUUID();
        driver.vehicleModel = "Test sedan"; driver.serviceArea = "test-area"; driver.location = "Campus"; driver.available = true;
        repository.saveAndFlush(driver);
        var start = new CountDownLatch(1);
        var tx = new TransactionTemplate(manager);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> claim = () -> {
                start.await();
                return tx.execute(status -> repository.claim(driver.id, UUID.randomUUID()));
            };
            Future<Integer> first = pool.submit(claim);
            Future<Integer> second = pool.submit(claim);
            start.countDown();
            assertEquals(1, first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS));
        }
        Driver reserved = repository.findById(driver.id).orElseThrow();
        assertFalse(reserved.available);
        assertNotNull(reserved.reservedRide);
        assertEquals(0, tx.execute(status -> repository.releaseReservation(UUID.randomUUID())).intValue());
        assertEquals(reserved.reservedRide, repository.findById(driver.id).orElseThrow().reservedRide);
        assertEquals(1, tx.execute(status -> repository.releaseReservation(reserved.reservedRide)).intValue());
        assertEquals(0, tx.execute(status -> repository.releaseReservation(reserved.reservedRide)).intValue());
        assertTrue(repository.findById(driver.id).orElseThrow().available);
        repository.deleteById(driver.id);
    }
}
