package de.tum.moodtrip_backend.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Health Check", description = "Health check endpoint for monitoring service status")
public class HealthController {
    
    @Operation(
        summary = "Check backend health status",
        description = "Returns a simple message to confirm that the backend service is running and responsive"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Backend is running successfully")
    })
    @GetMapping("/custom-health")
    public Mono<String> healthCheck() {
        return Mono.just("backend is running");
    }
}
