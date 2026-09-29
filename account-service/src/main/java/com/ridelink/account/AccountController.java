package com.ridelink.account;

import com.ridelink.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.*;

@io.swagger.v3.oas.annotations.responses.ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid request", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiError.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Missing/invalid credentials or inactive account", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiError.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Role or ownership forbids this operation", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiError.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="404", description="Resource not found", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiError.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409", description="Duplicate, concurrent update or invalid business state", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiError.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503", description="Required downstream service unavailable", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiError.class)))
})
@RestController
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service=service; }
    public record Registration(@Email @NotBlank @Size(max=254) String email, @NotBlank @Size(min=12,max=72) String password,
        @NotBlank @Size(max=100) String name, @NotNull Account.Role role) {}
    public record Login(@NotBlank String email,@NotBlank @Size(max=72) String password) {}
    public record Profile(@NotBlank @Size(max=100) String name) {}
    public record Status(@NotNull Boolean active) {}
    public record RoleChange(@NotNull Account.Role role) {}
    public record View(UUID id,String email,String name,Account.Role role,boolean active) {
        static View of(Account a) { return new View(a.id,a.email,a.name,a.role,a.active); }
    }
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED) @SecurityRequirements
    public View register(@Valid @RequestBody Registration r) { return View.of(service.register(r.email(),r.password(),r.name(),r.role())); }
    @PostMapping("/auth/login") @SecurityRequirements
    public Map<String,Object> login(@Valid @RequestBody Login r) { return service.login(r.email(),r.password()); }
    @GetMapping("/profiles/me")
    public View profile(@AuthenticationPrincipal Jwt jwt) { return View.of(service.get(Caller.from(jwt).id())); }
    @PatchMapping("/profiles/me")
    public View update(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Profile r) { return View.of(service.profile(Caller.from(jwt).id(),r.name())); }
    @PatchMapping("/accounts/{id}/status") @PreAuthorize("hasRole('ADMIN')")
    public View status(@PathVariable UUID id,@Valid @RequestBody Status r,@AuthenticationPrincipal Jwt jwt) { return View.of(service.status(id,r.active(),Caller.from(jwt))); }
    @PatchMapping("/accounts/{id}/role") @PreAuthorize("hasRole('ADMIN')")
    public View role(@PathVariable UUID id,@Valid @RequestBody RoleChange r,@AuthenticationPrincipal Jwt jwt) { return View.of(service.role(id,r.role(),Caller.from(jwt))); }
    @GetMapping("/internal/accounts/{id}") @SecurityRequirements({@SecurityRequirement(name="serviceKey")})
    public Contracts.AccountState state(@PathVariable UUID id) { var a=service.get(id); return new Contracts.AccountState(a.id,a.role.name(),a.active); }
}
