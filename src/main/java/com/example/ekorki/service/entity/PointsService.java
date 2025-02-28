package com.example.ekorki.service.entity;

import com.example.ekorki.dto.pointsOffer.PointsOfferDTO;
import com.example.ekorki.entity.PaymentHistoryEntity;
import com.example.ekorki.entity.PointsOfferEntity;
import com.example.ekorki.entity.RoleEntity;
import com.example.ekorki.entity.UserEntity;
import com.example.ekorki.exception.ApiException;
import com.example.ekorki.repository.PointsOfferRepository;
import com.example.ekorki.repository.UserRepository;
import com.example.ekorki.service.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PointsService {
    private final PointsOfferRepository pointsOfferRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PaymentHistoryService paymentHistoryService;
    private final EmailService emailService;

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

    @Caching(evict = {
            @CacheEvict(value = "users", key = "#loggedInUserId"),
            @CacheEvict(value = "userProfiles", key = "#loggedInUserId"),
            @CacheEvict(value = "courses", allEntries = true)
    })
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
        deductPoints(user.getId(), offer.getPoints());

        paymentHistoryService.addTransaction(
                loggedInUserId,
                PaymentHistoryEntity.TransactionType.POINTS_WITHDRAWAL,
                -offer.getPoints(),
                "Withdraw points",
                null,
                null
        );

        emailService.sendPointsWithdrawalConfirmation(
                user.getEmail(),
                offer.getPoints(),
                offer.getPrice(),
                user.getPoints()
        );


        return true;
    }

    @Caching(evict = {
            @CacheEvict(value = "users", key = "#loggedInUserId"),
            @CacheEvict(value = "userProfiles", key = "#loggedInUserId"),
            @CacheEvict(value = "courses", allEntries = true)
    })
    @Transactional
    public boolean buyPoints(Long offerId, Long loggedInUserId) {
        PointsOfferEntity offer = pointsOfferRepository.findByIdAndOfferType(offerId, PointsOfferEntity.OfferType.BUY)
                .orElseThrow(() -> new ApiException("Invalid or inactive offer"));

        UserEntity user = userRepository.findById(loggedInUserId)
                .orElseThrow(() -> new ApiException("User not found"));

        // Tutaj normalnie byłaby integracja z systemem płatności
        // Zakładamy, że płatność przeszła pomyślnie

        addPoints(loggedInUserId, offer.getPoints());

        if (!roleService.getUserRoles(loggedInUserId).contains(RoleEntity.Role.VERIFIED)) {
            throw new ApiException("Only verified can buy points");
        }

        paymentHistoryService.addTransaction(
                loggedInUserId,
                PaymentHistoryEntity.TransactionType.POINTS_PURCHASE,
                offer.getPoints(),
                "Purchase of Points",
                null,
                null
        );

        emailService.sendPointsPurchaseConfirmation(
                user.getEmail(),
                offer.getPoints(),
                offer.getPrice(),
                user.getPoints()
        );

        return true;
    }

    @Caching(evict = {
            @CacheEvict(value = "users", key = "#userId"),
            @CacheEvict(value = "userProfiles", key = "#userId"),
            @CacheEvict(value = "courses", allEntries = true)
    })
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

    @Caching(evict = {
            @CacheEvict(value = "users", key = "#userId"),
            @CacheEvict(value = "userProfiles", key = "#userId"),
            @CacheEvict(value = "courses", allEntries = true)
    })
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
