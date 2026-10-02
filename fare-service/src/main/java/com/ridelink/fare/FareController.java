package com.ridelink.fare;

import com.ridelink.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
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
public class FareController {
    private final FareService service;
    public FareController(FareService service) { this.service=service; }
    public record Estimate(@NotBlank @Size(max=120) String pickup,@NotBlank @Size(max=120) String destination,
        @NotNull @DecimalMin("0.1") @DecimalMax("500") @Digits(integer=3,fraction=2) BigDecimal distanceKm) {}
    public record ChargeRequest(@NotNull UUID rideId,@NotNull UUID passengerId,
        @NotNull @DecimalMin("0.1") @DecimalMax("500") @Digits(integer=3,fraction=2) BigDecimal distanceKm) {}
    public record Pay(@NotNull UUID rideId,@NotBlank @Size(max=80) String requestKey,@NotNull Boolean simulateFailure) {}
    @PostMapping("/fare-estimates")
    public Contracts.FareQuote estimate(@Valid @RequestBody Estimate r) { return service.estimate(r.distanceKm()); }
    @PostMapping("/internal/fare-estimates") @SecurityRequirements({@SecurityRequirement(name="serviceKey")})
    public Contracts.FareQuote internalEstimate(@Valid @RequestBody Estimate r) { return service.estimate(r.distanceKm()); }
    @PostMapping("/internal/charges") @SecurityRequirements({@SecurityRequirement(name="serviceKey")})
    public Contracts.ChargeView charge(@Valid @RequestBody ChargeRequest r) { return service.charge(r.rideId(),r.passengerId(),r.distanceKm()); }
    @GetMapping("/charges/{rideId}")
    public Contracts.ChargeView getCharge(@PathVariable UUID rideId,@AuthenticationPrincipal Jwt jwt) { return service.getCharge(rideId,Caller.from(jwt)); }
    @PostMapping("/payments") @ResponseStatus(HttpStatus.CREATED)
    public Payment pay(@Valid @RequestBody Pay r,@AuthenticationPrincipal Jwt jwt) { return service.pay(r.rideId(),r.requestKey(),r.simulateFailure(),Caller.from(jwt)); }
    @GetMapping("/payments/{id}")
    public Payment payment(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) { return service.getPayment(id,Caller.from(jwt)); }
    @GetMapping("/receipts/{paymentId}")
    public Map<String,Object> receipt(@PathVariable UUID paymentId,@AuthenticationPrincipal Jwt jwt) { return service.receipt(paymentId,Caller.from(jwt)); }
}
