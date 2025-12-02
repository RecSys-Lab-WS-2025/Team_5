package de.tum.moodtrip_backend.adapter_database.controller;


import de.tum.moodtrip_backend.core.model.UserProfile;
import de.tum.moodtrip_backend.core.service.UserDomainService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.tum.moodtrip_backend.core.model.SpotifyTokenDomain;
import de.tum.moodtrip_backend.core.port.SpotifyTokenPort;
import de.tum.moodtrip_backend.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/token")
@Tag(name = "Spotify Token Management", description = "APIs for managing Spotify access and refresh tokens")
public class SpotifyTokenController {
    private final SpotifyTokenPort spotifyTokenPort;
    private final JwtService jwtService;
    private final UserDomainService userDomainService;

    public SpotifyTokenController(SpotifyTokenPort spotifyTokenPort, JwtService jwtService, UserDomainService userDomainService) {
        this.spotifyTokenPort = spotifyTokenPort;
        this.jwtService = jwtService;
        this.userDomainService = userDomainService;
    }

    @Operation(
        summary = "Create Spotify token",
        description = "Stores a new Spotify access token and refresh token for a user",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Spotify token created successfully"
        )
    })
    @PostMapping
    public Mono<SpotifyTokenDomain> create(
            @RequestBody CreateSpotifyTokenRequest request,
            Authentication authentication) {


        jwtService.extractUserId(authentication);

        if (request.accessToken == null || request.accessToken.isBlank()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AccessToken cannot be null or empty"
            ));
        }

        SpotifyTokenDomain domain = new SpotifyTokenDomain(
                null,
                request.accessToken,
                request.refreshToken,
                request.expiresIn,
                System.currentTimeMillis() / 1000,
                request.spotifyUserId,
                request.spotifyEmail,
                request.spotifyDisplayName
        );
        return spotifyTokenPort.save(domain);
    }

    @Operation(
        summary = "Get Spotify token by ID",
        description = "Retrieves a Spotify token by its database ID",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Spotify token found"
        )
    })
    @GetMapping("/{id}")
    public Mono<SpotifyTokenDomain> getById(
            @Parameter(description = "Token ID", required = true) @PathVariable Long id,
            Authentication authentication) {


        jwtService.extractUserId(authentication);

        return spotifyTokenPort.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Spotify token not found for id: " + id
                )));
    }

    @Operation(
        summary = "Get Spotify token by Spotify user ID",
        description = "Retrieves a Spotify token using the Spotify user ID",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Spotify token found"
        )
    })
    @GetMapping("/spotify-user/{spotifyUserId}")
    public Mono<SpotifyTokenDomain> getBySpotifyUserId(
            @Parameter(description = "Spotify user ID", required = true) @PathVariable String spotifyUserId,
            Authentication authentication) {


        jwtService.extractUserId(authentication);

        return spotifyTokenPort.findBySpotifyUserId(spotifyUserId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Spotify token not found for spotifyUserId: " + spotifyUserId
                )));
    }

    @Operation(
        summary = "Delete Spotify token",
        description = "Deletes a Spotify token. Users can only delete tokens linked to their account.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Spotify token deleted successfully"
        )
    })
    @DeleteMapping("/{id}")
    public Mono<Void> deleteById(
            @Parameter(description = "Token ID", required = true) @PathVariable Long id,
            Authentication authentication) {
        long userId = jwtService.extractUserId(authentication);

        return spotifyTokenPort.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Spotify token not found for id: " + id
                )))
                .flatMap(token ->
                        userDomainService.findById(userId)
                                .flatMap(user -> {
                                    if (user.spotifyTokenId() == null ||
                                            !user.spotifyTokenId().equals(id)) {
                                        return Mono.error(new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "You are not authorized to delete this Spotify token"
                                        ));
                                    }
                                    return spotifyTokenPort.deleteById(id);
                                })
                );
    }


    public record CreateSpotifyTokenRequest(
            String accessToken,
            String refreshToken,
            Long expiresIn,
            String spotifyUserId,
            String spotifyEmail,
            String spotifyDisplayName
    ) {
    }
}
