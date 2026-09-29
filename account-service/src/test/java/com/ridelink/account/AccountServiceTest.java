package com.ridelink.account;

import com.ridelink.common.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class AccountServiceTest {
    AccountRepository repository; AccountService service;
    @BeforeEach void setup() {
        repository=mock(AccountRepository.class); service=new AccountService(repository,mock(JwtEncoder.class));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void registrationNormalizesEmailAndHashesPassword() {
        Account a=service.register(" PERSON@EXAMPLE.TEST ","A-long-test-password"," Name ",Account.Role.PASSENGER);
        assertEquals("person@example.test",a.email); assertEquals("Name",a.name); assertTrue(a.active);
        assertNotEquals("A-long-test-password",a.passwordHash);
        assertTrue(new BCryptPasswordEncoder().matches("A-long-test-password",a.passwordHash));
    }
    @Test void registrationCannotGrantAdmin() {
        assertEquals(403,assertThrows(ApiException.class,()->service.register("x@example.test","long-enough-password","X",Account.Role.ADMIN)).status);
        verify(repository,never()).save(any());
    }
    @Test void duplicateEmailIsConflict() {
        when(repository.findByEmail("x@example.test")).thenReturn(Optional.of(new Account()));
        assertEquals(409,assertThrows(ApiException.class,()->service.register("X@example.test","long-enough-password","X",Account.Role.DRIVER)).status);
    }
    @Test void unicodePasswordOverBcryptLimitIsRejected() {
        assertEquals(400,assertThrows(ApiException.class,()->service.register("x@example.test","Ã©".repeat(40),"X",Account.Role.DRIVER)).status);
    }
    @Test void unknownLoginIsUnauthorized() {
        assertEquals(401,assertThrows(ApiException.class,()->service.login("absent@example.test","wrong")).status);
    }
    @Test void wrongPasswordIsUnauthorized() {
        Account a=new Account();a.passwordHash=new BCryptPasswordEncoder().encode("correct-long-password");
        when(repository.findByEmail("x@example.test")).thenReturn(Optional.of(a));
        assertEquals(401,assertThrows(ApiException.class,()->service.login("x@example.test","wrong")).status);
    }
    @Test void disabledAccountCannotLogin() {
        Account a=new Account();a.active=false;a.passwordHash=new BCryptPasswordEncoder().encode("correct-long-password");
        when(repository.findByEmail("x@example.test")).thenReturn(Optional.of(a));
        assertEquals(403,assertThrows(ApiException.class,()->service.login("x@example.test","correct-long-password")).status);
    }
    @Test void adminCannotDisableThemself() {
        UUID id=UUID.randomUUID();
        assertEquals(409,assertThrows(ApiException.class,()->service.status(id,false,new Caller(id,"ADMIN"))).status);
    }
    @Test void profileUpdateDoesNotChangeRole() {
        Account a=new Account();a.id=UUID.randomUUID();a.role=Account.Role.PASSENGER;
        when(repository.findById(a.id)).thenReturn(Optional.of(a));
        assertEquals(Account.Role.PASSENGER,service.profile(a.id,"Updated").role);
    }
}
