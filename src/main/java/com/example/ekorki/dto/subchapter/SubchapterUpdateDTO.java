package com.example.ekorki.dto.subchapter;

import com.example.ekorki.dto.contentItem.ContentItemUpdateDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubchapterUpdateDTO {
    private Long id; // null jeśli nowy podrozdział
    private Optional<String> name;
    private Optional<Integer> order;
    private Optional<List<ContentItemUpdateDTO>> content;
    private Optional<Boolean> deleted; // true jeśli podrozdział ma zostać usunięty
}
