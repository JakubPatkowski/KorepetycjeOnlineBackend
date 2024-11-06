package com.example.demo.dto.contentItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentItemUpdateDTO {
    private Long id; // null jeśli nowy element
    private Optional<String> type;
    private Optional<Integer> order;

    // Pola dla typu 'text'
    private Optional<String> textContent;
    private Optional<String> fontSize;
    private Optional<String> fontWeight;
    private Optional<Boolean> italics;
    private Optional<Boolean> emphasis;

    // Pole dla typu 'quiz'
    private Optional<String> quizData;

    // Pole wskazujące, czy element ma zostać usunięty
    private Optional<Boolean> deleted;

    // Pole wskazujące, czy plik ma zostać zaktualizowany
    private Optional<Boolean> updateFile;
}
