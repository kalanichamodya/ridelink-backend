package com.ridelink.account;

import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.ridelink.common.MongoConfig;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Import(MongoConfig.class)
@EnabledIfEnvironmentVariable(named="TEST_MONGODB_URI", matches=".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountRepositoryTest {
    private static final String DATABASE = "ridelink_test_account_" + UUID.randomUUID().toString().replace("-", "");
    @DynamicPropertySource static void mongoProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.data.mongodb.uri", () -> System.getenv("TEST_MONGODB_URI"));
        properties.add("spring.data.mongodb.database", () -> DATABASE);
        properties.add("spring.data.mongodb.uuid-representation", () -> "standard");
    }
    @Autowired MongoTemplate mongo;
    @Autowired PlatformTransactionManager manager;
    @AfterAll void cleanup() {
        assertEquals(DATABASE, mongo.getDb().getName());
        mongo.getDb().drop();
    }
    @Autowired AccountRepository repository;
    @Test void uniqueEmailAndOptimisticUpdatesAreEnforced() {
        Account first=new Account(); first.id=UUID.randomUUID(); first.email="test-"+UUID.randomUUID()+"@example.test";
        first.name="Test"; first.passwordHash="test-only"; first.role=Account.Role.PASSENGER;
        repository.save(first);
        Account duplicate=new Account(); duplicate.id=UUID.randomUUID(); duplicate.email=first.email;
        assertThrows(DuplicateKeyException.class,()->repository.save(duplicate));
        Account stale=repository.findById(first.id).orElseThrow();
        first.name="Updated"; repository.save(first);
        stale.active=false; assertThrows(OptimisticLockingFailureException.class,()->repository.save(stale));
        assertEquals(Account.Role.PASSENGER,repository.findByEmail(first.email).orElseThrow().role);
    }
}
