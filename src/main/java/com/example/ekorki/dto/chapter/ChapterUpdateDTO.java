package com.example.ekorki.dto.chapter;

import com.example.ekorki.dto.subchapter.SubchapterUpdateDTO;
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
public class ChapterUpdateDTO {
    private Long id; // null jeśli nowy rozdział
    private Optional<String> name;
    private Optional<Integer> order;
    private Optional<List<SubchapterUpdateDTO>> subchapters;
    private Optional<Boolean> deleted; // true jeśli rozdział ma zostać usunięty
}
