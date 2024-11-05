package com.example.demo.controller;

import com.example.demo.dto.http.HttpResponseDTO;
import com.example.demo.dto.pointsOffer.PointsOfferDTO;
import com.example.demo.exception.ApiException;
import com.example.demo.model.UserPrincipals;
import com.example.demo.service.entity.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static java.util.Map.of;

import static java.time.LocalDate.now;

@RestController
@RequestMapping(path = "/points")
@RequiredArgsConstructor
public class PointsController {
    private final PointsService pointsService;

    @GetMapping("get-offers")
    public ResponseEntity<HttpResponseDTO> getActiveOffers() {
        List<PointsOfferDTO> offers = pointsService.getActiveOffers();

        return ResponseEntity.ok(
                HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .data(of("offers", offers))
                        .message("Active offers retrieved successfully")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build()
        );
    }

    @PostMapping("/buy")
    public ResponseEntity<HttpResponseDTO> buyPoints(@RequestBody Map<String, Long> body, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        Long offerId = body.get("offerId");

        if (offerId == null) {
            return ResponseEntity.badRequest().body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message("Offer ID is required")
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .build());
        }

        try {
            boolean purchased = pointsService.buyPoints(offerId, loggedInUserId);

            if (purchased) {
                int currentPoints = pointsService.getUserPoints(loggedInUserId);
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Points purchased successfully")
                        .data(of("currentPoints", currentPoints))
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.badRequest().body(
                        HttpResponseDTO.builder()
                                .timestamp(now().toString())
                                .message("Failed to purchase points")
                                .status(HttpStatus.BAD_REQUEST)
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .build());
            }
        } catch (ApiException e) {
            return ResponseEntity.badRequest().body(
                    HttpResponseDTO.builder()
                            .timestamp(now().toString())
                            .message(e.getMessage())
                            .status(HttpStatus.BAD_REQUEST)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .build());
        }
    }
}
