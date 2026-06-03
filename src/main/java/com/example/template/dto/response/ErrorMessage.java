package com.example.template.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Berisi pesan error dalam dua bahasa (English dan Indonesia).
 */
@Getter
@Builder
public class ErrorMessage {

    private String english;
    private String indonesian;
}
