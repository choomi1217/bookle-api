package com.bookle.wordbook.service;

import com.bookle.wordbook.dto.AiSentenceResponse;
import com.bookle.wordbook.dto.AiWordResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Claude API 호출 서비스. v1 단계에서는 시그니처·주입만 구성하고,
 * 실제 호출 로직은 ANTHROPIC_API_KEY 수령 후 구현한다 (docs/01-backend.md 참조).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService {

    private final RestClient claudeRestClient;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model}")
    private String model;

    @Value("${anthropic.max-tokens}")
    private int maxTokens;

    public AiWordResponse generateWordMeaning(String word) {
        throw new UnsupportedOperationException("ClaudeService.generateWordMeaning: 키 수령 후 구현 예정");
    }

    public AiSentenceResponse generateSentenceTranslation(String sentence) {
        throw new UnsupportedOperationException("ClaudeService.generateSentenceTranslation: 키 수령 후 구현 예정");
    }
}
