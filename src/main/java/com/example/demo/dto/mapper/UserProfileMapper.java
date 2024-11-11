package com.example.demo.dto.mapper;

import com.example.demo.dto.userProfile.UserProfileResponseDTO;
import com.example.demo.entity.UserProfileEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UserProfileMapper {

    public UserProfileResponseDTO mapToDTO(UserProfileEntity entity) {
        if (entity == null) {
            return null;
        }

        UserProfileResponseDTO dto = new UserProfileResponseDTO();
        dto.setId(entity.getId());
        dto.setFullName(entity.getFullName());
        dto.setUserId(entity.getUserId());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setBadgesVisible(entity.getBadgesVisible());

        if (entity.getPicture() != null && entity.getPictureMimeType() != null) {
            Map<String, Object> pictureData = new HashMap<>();
            pictureData.put("data", entity.getPicture());
            pictureData.put("mimeType", entity.getPictureMimeType());
            dto.setPicture(pictureData);
        }

        return dto;
    }
}
