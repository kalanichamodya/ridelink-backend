package com.ridelink.account;

import com.ridelink.common.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final AccountRepository repository;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
    private final JwtEncoder encoder;
    public AccountService(AccountRepository repository, JwtEncoder encoder) { this.repository=repository; this.encoder=encoder; }
    public Account register(String email, String password, String name, Account.Role role) {
        if (role == Account.Role.ADMIN) throw new ApiException(403,"FORBIDDEN","Public registration cannot create an administrator");
        return create(email,password,name,role);
    }
    Account create(String email,String password,String name,Account.Role role) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (repository.findByEmail(normalized).isPresent()) throw ApiException.conflict("Email is already registered");
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ApiException(400,"INVALID_INPUT","Password exceeds BCrypt's 72-byte limit");
        Account a = new Account(); a.id=UUID.randomUUID(); a.email=normalized; a.name=name.trim(); a.role=role; a.passwordHash=passwords.encode(password);
        try { return repository.save(a); }
        catch (DataIntegrityViolationException e) { throw ApiException.conflict("Email is already registered"); }
    }
    public Map<String,Object> login(String email,String password) {
        Account a = repository.findByEmail(email.trim().toLowerCase(Locale.ROOT)).orElseThrow(() -> new ApiException(401,"INVALID_CREDENTIALS","Invalid email or password"));
        if (!passwords.matches(password,a.passwordHash)) throw new ApiException(401,"INVALID_CREDENTIALS","Invalid email or password");
        if (!a.active) throw new ApiException(403,"ACCOUNT_INACTIVE","Account is inactive");
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("ridelink-account").subject(a.id.toString()).issuedAt(now).expiresAt(now.plus(1,ChronoUnit.HOURS)).claim("role",a.role.name()).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return Map.of("accessToken",token,"tokenType","Bearer","expiresIn",3600,"userId",a.id,"role",a.role);
    }
    public Account get(UUID id) { return repository.findById(id).orElseThrow(() -> ApiException.notFound("Account")); }
    public Account profile(UUID id,String name) { var a=get(id); a.name=name.trim(); return repository.save(a); }
    public Account status(UUID id,boolean active,Caller caller) {
        if (id.equals(caller.id()) && !active) throw ApiException.conflict("Administrator cannot disable their own account");
        var a=get(id); a.active=active; return repository.save(a);
    }
    public Account role(UUID id,Account.Role role,Caller caller) {
        if (id.equals(caller.id())) throw ApiException.conflict("Administrator cannot change their own role");
        var a=get(id); a.role=role; return repository.save(a);
    }
}
