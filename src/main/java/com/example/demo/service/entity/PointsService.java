package com.example.demo.service.entity;

import com.example.demo.dto.pointsOffer.PointsOfferDTO;
import com.example.demo.entity.PointsOfferEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ApiException;
import com.example.demo.repository.PointsOfferRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PointsService {
    private final PointsOfferRepository pointsOfferRepository;
    private final UserRepository userRepository;

    public List<PointsOfferDTO> getActiveOffers() {
        return pointsOfferRepository.findByActiveTrue()
                .stream()
                .map(offer -> new PointsOfferDTO(
                        offer.getId(),
                        offer.getPoints(),
                        offer.getPrice()
                ))
                .collect(Collectors.toList());
    }

    public Optional<PointsOfferEntity> getActiveOfferById(Long id) {
        return pointsOfferRepository.findById(id)
                .filter(PointsOfferEntity::getActive);
    }

    @Transactional
    public boolean buyPoints(Long offerId, Long loggedInUserId) {
        PointsOfferEntity offer = getActiveOfferById(offerId)
                .orElseThrow(() -> new ApiException("Invalid or inactive offer"));

        UserEntity user = userRepository.findById(loggedInUserId)
                .orElseThrow(() -> new ApiException("User not found"));

        // Tutaj normalnie byłaby integracja z systemem płatności
        // Zakładamy, że płatność przeszła pomyślnie

        user.setPoints(user.getPoints() + offer.getPoints());
        userRepository.save(user);

        return true;
    }

    @Transactional
    public void deductPoints(Long userId, int points) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        if (user.getPoints() < points) {
            throw new ApiException("Insufficient points balance");
        }

        user.setPoints(user.getPoints() - points);
        userRepository.save(user);
    }

    @Transactional
    public void addPoints(Long userId, int points) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        user.setPoints(user.getPoints() + points);
        userRepository.save(user);
    }

    public boolean hasEnoughPoints(Long userId, int requiredPoints) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));

        return user.getPoints() >= requiredPoints;
    }

    public int getUserPoints(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        return user.getPoints();
    }
}
