package com.ridelink.ride;

import com.ridelink.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
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
public class RideController {
    private final RideService service;
    public RideController(RideService service) { this.service=service; }
    public record Request(@NotBlank @Size(max=120) String pickup,@NotBlank @Size(max=120) String destination,
        @NotBlank @Size(max=80) String serviceArea,@NotNull @DecimalMin("0.1") @DecimalMax("500") @Digits(integer=3,fraction=2) BigDecimal distanceKm,
        @NotBlank @Size(max=80) String requestKey) {}
    @PostMapping("/rides") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Create REQUESTED ride; requestKey is a passenger-scoped idempotency key")
    public Ride create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Request r) {
        return service.create(Caller.from(jwt),r.requestKey(),r.pickup(),r.destination(),r.serviceArea(),r.distanceKm());
    }
    @PostMapping("/rides/{id}/assign")
    public Ride assign(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.assign(id,Caller.from(jwt)); }
    @PostMapping("/rides/{id}/accept")
    public Ride accept(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.move(id,Caller.from(jwt),RideState.ACCEPTED); }
    @PostMapping("/rides/{id}/start")
    public Ride start(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.move(id,Caller.from(jwt),RideState.IN_PROGRESS); }
    @PostMapping("/rides/{id}/complete")
    public Ride complete(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.move(id,Caller.from(jwt),RideState.COMPLETED); }
    @PostMapping("/rides/{id}/cancel")
    public Ride cancel(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.move(id,Caller.from(jwt),RideState.CANCELLED); }
    @GetMapping("/rides/{id}")
    public Ride get(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.get(id,Caller.from(jwt)); }
    @GetMapping("/rides")
    public List<Ride> list(@AuthenticationPrincipal Jwt jwt) { return service.list(Caller.from(jwt)); }
    @GetMapping("/rides/{id}/history")
    public List<RideEvent> history(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.history(id,Caller.from(jwt)); }
    @GetMapping("/internal/rides/{id}") @SecurityRequirements({@SecurityRequirement(name="serviceKey")})
    public Contracts.RideSummary internal(@PathVariable UUID id) { return service.summary(id); }
}
