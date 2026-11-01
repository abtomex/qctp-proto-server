package ru.abtomex.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class QctpMessage {

    private final String fileName;
    private final int totalPages;
    private final int page;
    private final String content;


}
