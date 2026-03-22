package com.hnust.examai.module.flashcard;

import com.hnust.examai.common.result.R;
import com.hnust.examai.module.flashcard.dto.*;
import com.hnust.examai.module.flashcard.FlashcardService.DeckDetailResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 抽认卡 Controller
 * <p>
 * Base: /api/flashcards
 * </p>
 */
@RestController
@RequestMapping("/api/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    private Long getUserId(UserDetails userDetails) {
        return Long.parseLong(userDetails.getUsername());
    }

    @PostMapping("/generate")
    public R<Map<String, Long>> generate(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GenerateFlashcardRequest request) {
        Long deckId = flashcardService.generateFromNote(getUserId(userDetails), request.getNoteId());
        return R.ok(Map.of("deckId", deckId));
    }

    @GetMapping("/decks")
    public R<List<FlashcardDeckVO>> listDecks(@AuthenticationPrincipal UserDetails userDetails) {
        return R.ok(flashcardService.listDecks(getUserId(userDetails)));
    }

    @GetMapping("/decks/{id}")
    public R<DeckDetailResult> getDeck(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return R.ok(flashcardService.getDeckDetail(getUserId(userDetails), id));
    }

    @DeleteMapping("/decks/{id}")
    public R<Void> deleteDeck(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        flashcardService.deleteDeck(getUserId(userDetails), id);
        return R.ok();
    }

    @PutMapping("/{cardId}")
    public R<Void> updateCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateCardRequest request) {
        flashcardService.updateCard(getUserId(userDetails), cardId, request);
        return R.ok();
    }

    @GetMapping("/today-review")
    public R<List<FlashcardVO>> todayReview(@AuthenticationPrincipal UserDetails userDetails) {
        return R.ok(flashcardService.todayReview(getUserId(userDetails)));
    }

    @PostMapping("/{cardId}/review")
    public R<Void> submitReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long cardId,
            @Valid @RequestBody FlashcardReviewRequest request) {
        flashcardService.submitReview(getUserId(userDetails), cardId, request);
        return R.ok();
    }
}
