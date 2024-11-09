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
    private Optional<String> text;
    private Optional<String> fontSize;
    private Optional<Boolean> bolder;
    private Optional<Boolean> italics;
    private Optional<Boolean> underline;
    private Optional<String> textColor;

    // Pole dla typu 'quiz'
    private Optional<String> quizContent;

    // Pole wskazujące, czy element ma zostać usunięty
    private Optional<Boolean> deleted;

    // Pole wskazujące, czy plik ma zostać zaktualizowany
    private Optional<Boolean> updateFile;
}
