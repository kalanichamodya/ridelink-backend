package com.ridelink.driver;

import com.ridelink.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
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
public class DriverController {
    private final DriverService service;
    public DriverController(DriverService service) { this.service=service; }
    public record Details(@NotBlank @Size(max=20) String plate,@NotBlank @Size(max=100) String vehicleModel,
        @NotBlank @Size(max=80) String serviceArea,@NotBlank @Size(max=120) String location) {}
    public record Availability(@NotNull Boolean available) {}
    public record View(UUID id,String plate,String vehicleModel,String serviceArea,String location,boolean available,UUID reservedRide) {
        static View of(Driver d) { return new View(d.id,d.plate,d.vehicleModel,d.serviceArea,d.location,d.available,d.reservedRide); }
    }
    public record ReservationRequest(@NotNull UUID rideId,@NotBlank @Size(max=80) String serviceArea) {}
    public record ReleaseRequest(@NotNull UUID rideId) {}
    @PutMapping("/drivers/me") @PreAuthorize("hasRole('DRIVER')")
    public View save(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Details r) {
        return View.of(service.save(Caller.from(jwt).id(),r.plate(),r.vehicleModel(),r.serviceArea(),r.location()));
    }
    @GetMapping("/drivers/me") @PreAuthorize("hasRole('DRIVER')")
    public View me(@AuthenticationPrincipal Jwt jwt) { return View.of(service.get(Caller.from(jwt).id())); }
    @PatchMapping("/drivers/me/availability") @PreAuthorize("hasRole('DRIVER')")
    public View availability(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Availability r) { return View.of(service.availability(Caller.from(jwt).id(),r.available())); }
    @GetMapping("/drivers/eligible") @PreAuthorize("hasAnyRole('PASSENGER','ADMIN')")
    public List<View> eligible(@RequestParam @NotBlank @Size(max=80) String serviceArea) { return service.eligible(serviceArea).stream().map(View::of).toList(); }
    @PostMapping("/internal/reservations") @SecurityRequirements({@SecurityRequirement(name="serviceKey")})
    public Contracts.Reservation reserve(@Valid @RequestBody ReservationRequest r) { return service.reserve(r.rideId(),r.serviceArea()); }
    @PostMapping("/internal/reservations/release") @SecurityRequirements({@SecurityRequirement(name="serviceKey")})
    public Map<String,String> release(@Valid @RequestBody ReleaseRequest r) { service.release(r.rideId()); return Map.of("status","RELEASED"); }
}
