package com.example.ekorki.controller;

import com.example.ekorki.dto.http.HttpResponseDTO;
import com.example.ekorki.dto.pointsOffer.PointsOfferDTO;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.model.UserPrincipals;
import com.example.ekorki.service.entity.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Map.of;

import static java.time.LocalDate.now;

@RestController
@RequestMapping(path = "/points")
@RequiredArgsConstructor
public class PointsController {
    private final PointsService pointsService;

    @GetMapping("get-offers")
    public ResponseEntity<HttpResponseDTO> getBuyOffers() {
        List<PointsOfferDTO> offers = pointsService.getBuyOffers();

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

    @GetMapping("/get-withdrawal-offers")
    public ResponseEntity<HttpResponseDTO> getWithdrawalOffers() {
        List<PointsOfferDTO> offers = pointsService.getWithdrawalOffers();

        return ResponseEntity.ok(
                HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .data(of("offers", offers))
                        .message("Withdrawal offers retrieved successfully")
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build()
        );
    }

    @PostMapping("/buy/{offerId}")
    public ResponseEntity<HttpResponseDTO> buyPoints(@PathVariable Long offerId, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();



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

    @PostMapping("/withdraw/{offerId}")
    public ResponseEntity<HttpResponseDTO> withdrawPoints(
            @PathVariable Long offerId,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long loggedInUserId = ((UserPrincipals) userDetails).getId();

        try {
            boolean withdrawn = pointsService.withdrawPoints(offerId, loggedInUserId);
            if (withdrawn) {
                int currentPoints = pointsService.getUserPoints(loggedInUserId);
                return ResponseEntity.ok(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Points withdrawn successfully")
                        .data(of("currentPoints", currentPoints))
                        .status(HttpStatus.OK)
                        .statusCode(HttpStatus.OK.value())
                        .build());
            } else {
                return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                        .timestamp(now().toString())
                        .message("Failed to withdraw points")
                        .status(HttpStatus.BAD_REQUEST)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .build());
            }
        } catch (ApiException e) {
            return ResponseEntity.badRequest().body(HttpResponseDTO.builder()
                    .timestamp(now().toString())
                    .message(e.getMessage())
                    .status(HttpStatus.BAD_REQUEST)
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
        }
    }
}
