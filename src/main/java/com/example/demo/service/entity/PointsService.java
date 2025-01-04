package com.example.demo.service.entity;

import com.example.demo.dto.pointsOffer.PointsOfferDTO;
import com.example.demo.entity.PointsOfferEntity;
import com.example.demo.entity.RoleEntity;
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
    private final RoleService roleService;

    public List<PointsOfferDTO> getBuyOffers() {
        return pointsOfferRepository.findByOfferType(PointsOfferEntity.OfferType.BUY)
                .stream()
                .map(offer -> new PointsOfferDTO(
                        offer.getId(),
                        offer.getPoints(),
                        offer.getPrice()
                ))
                .collect(Collectors.toList());
    }

    public List<PointsOfferDTO> getWithdrawalOffers() {
        return pointsOfferRepository.findByOfferType(PointsOfferEntity.OfferType.SELL)
                .stream()
                .map(offer -> new PointsOfferDTO(
                        offer.getId(),
                        offer.getPoints(),
                        offer.getPrice()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean withdrawPoints(Long offerId, Long loggedInUserId) {
        PointsOfferEntity offer = pointsOfferRepository.findByIdAndOfferType(offerId, PointsOfferEntity.OfferType.SELL)
                .orElseThrow(() -> new ApiException("Invalid withdrawal offer"));

        UserEntity user = userRepository.findById(loggedInUserId)
                .orElseThrow(() -> new ApiException("User not found"));

        // Sprawdź czy użytkownik jest nauczycielem
        if (!roleService.getUserRoles(loggedInUserId).contains(RoleEntity.Role.TEACHER)) {
            throw new ApiException("Only teachers can withdraw points");
        }

        // Sprawdź czy użytkownik ma wystarczająco punktów
        if (user.getPoints() < offer.getPoints()) {
            throw new ApiException("Insufficient points balance");
        }

        // Odejmij punkty
        user.setPoints(user.getPoints() - offer.getPoints());
        userRepository.save(user);

        return true;
    }

    @Transactional
    public boolean buyPoints(Long offerId, Long loggedInUserId) {
        PointsOfferEntity offer = pointsOfferRepository.findByIdAndOfferType(offerId, PointsOfferEntity.OfferType.BUY)
                .orElseThrow(() -> new ApiException("Invalid or inactive offer"));

        UserEntity user = userRepository.findById(loggedInUserId)
                .orElseThrow(() -> new ApiException("User not found"));

        // Tutaj normalnie byłaby integracja z systemem płatności
        // Zakładamy, że płatność przeszła pomyślnie

        addPoints(loggedInUserId, offer.getPoints());
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
