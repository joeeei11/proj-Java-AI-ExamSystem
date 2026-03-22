package com.hnust.examai.module.flashcard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hnust.examai.common.ai.DeepSeekClient;
import com.hnust.examai.common.exception.BizException;
import com.hnust.examai.common.result.ResultCode;
import com.hnust.examai.entity.Flashcard;
import com.hnust.examai.entity.FlashcardDeck;
import com.hnust.examai.entity.Note;
import com.hnust.examai.module.common.SrsService;
import com.hnust.examai.module.flashcard.dto.*;
import com.hnust.examai.module.note.NoteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 抽认卡 Service
 * <p>
 * 支持 AI 生成卡组、卡组管理、SRS 每日复习（复用 SrsService）
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final FlashcardDeckMapper deckMapper;
    private final FlashcardMapper flashcardMapper;
    private final NoteMapper noteMapper;
    private final DeepSeekClient deepSeekClient;
    private final SrsService srsService;
    private final ObjectMapper objectMapper;

    // ===== 公开方法 =====

    /**
     * AI 生成抽认卡：读取笔记内容 → 调用 DeepSeek → 解析 → 落库
     *
     * @param userId 当前用户 ID
     * @param noteId 来源笔记 ID
     * @return 新建卡组 ID
     */
    @Transactional
    public Long generateFromNote(Long userId, Long noteId) {
        // 1. 获取笔记内容
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BizException(ResultCode.NOTE_NOT_FOUND);
        }
        if (!note.getUserId().equals(userId)) {
            throw new BizException(ResultCode.NOTE_ACCESS_DENIED);
        }

        String noteContent = buildNoteContent(note);
        if (noteContent.isBlank()) {
            throw new BizException(ResultCode.REVIEW_NOTE_EMPTY);
        }

        // 2. 读取 Prompt 模板
        String systemPrompt = loadPrompt("flashcard_generate.txt");

        // 3. 调用 AI
        log.info("开始 AI 生成抽认卡，userId={}, noteId={}", userId, noteId);
        String aiResponse = deepSeekClient.chat(systemPrompt, "笔记内容：\n" + noteContent);

        // 4. 解析 AI 响应
        String deckTitle;
        List<Map<String, String>> cards;
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            deckTitle = root.path("title").asText(note.getTitle() + " - 抽认卡");
            JsonNode cardsNode = root.path("cards");
            cards = objectMapper.convertValue(cardsNode, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("解析 AI 抽认卡响应失败: {}", e.getMessage());
            throw new BizException(ResultCode.AI_PARSE_FAILED, "抽认卡生成响应解析失败");
        }

        if (cards == null || cards.isEmpty()) {
            throw new BizException(ResultCode.AI_PARSE_FAILED, "AI 未生成任何卡片");
        }

        // 5. 创建卡组
        FlashcardDeck deck = new FlashcardDeck();
        deck.setUserId(userId);
        deck.setNoteId(noteId);
        deck.setTitle(deckTitle);
        deck.setCardCount(cards.size());
        deckMapper.insert(deck);

        // 6. 批量创建卡片
        for (int i = 0; i < cards.size(); i++) {
            Map<String, String> cardData = cards.get(i);
            Flashcard card = new Flashcard();
            card.setDeckId(deck.getId());
            card.setUserId(userId);
            card.setFront(cardData.getOrDefault("front", ""));
            card.setBack(cardData.getOrDefault("back", ""));
            card.setSortOrder(i + 1);
            card.setReviewIntervalDays(1);
            card.setReviewCount(0);
            card.setConsecutiveCorrect(0);
            flashcardMapper.insert(card);
        }

        log.info("AI 生成抽认卡完成，userId={}, noteId={}, deckId={}, 卡片数={}",
                userId, noteId, deck.getId(), cards.size());
        return deck.getId();
    }

    /**
     * 卡组列表（含今日待复习数量）
     *
     * @param userId 当前用户 ID
     */
    public List<FlashcardDeckVO> listDecks(Long userId) {
        List<FlashcardDeck> decks = deckMapper.selectList(
                new LambdaQueryWrapper<FlashcardDeck>()
                        .eq(FlashcardDeck::getUserId, userId)
                        .orderByDesc(FlashcardDeck::getCreatedAt));

        // 查询今日待复习卡片，按卡组分组统计
        List<Flashcard> todayCards = flashcardMapper.selectTodayReview(userId);
        Map<Long, Long> todayCountByDeck = todayCards.stream()
                .collect(Collectors.groupingBy(Flashcard::getDeckId, Collectors.counting()));

        return decks.stream().map(deck -> {
            FlashcardDeckVO vo = new FlashcardDeckVO();
            vo.setId(deck.getId());
            vo.setNoteId(deck.getNoteId());
            vo.setTitle(deck.getTitle());
            vo.setCardCount(deck.getCardCount());
            vo.setTodayReviewCount(
                    todayCountByDeck.getOrDefault(deck.getId(), 0L).intValue());
            vo.setCreatedAt(deck.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 卡组详情 + 所有卡片
     *
     * @param userId 当前用户 ID
     * @param deckId 卡组 ID
     */
    public FlashcardDeckVO getDeck(Long userId, Long deckId) {
        FlashcardDeck deck = requireDeck(deckId, userId);

        List<Flashcard> cards = flashcardMapper.selectList(
                new LambdaQueryWrapper<Flashcard>()
                        .eq(Flashcard::getDeckId, deckId)
                        .orderByAsc(Flashcard::getSortOrder));

        FlashcardDeckVO vo = new FlashcardDeckVO();
        vo.setId(deck.getId());
        vo.setNoteId(deck.getNoteId());
        vo.setTitle(deck.getTitle());
        vo.setCardCount(deck.getCardCount());
        vo.setCreatedAt(deck.getCreatedAt());

        List<FlashcardVO> cardVOs = cards.stream().map(this::toCardVO).collect(Collectors.toList());
        // 使用反射注入 cards 字段较复杂，改用扩展 VO 方式在 Controller 层组装
        // 这里返回的 VO 含卡片列表通过 DeckDetailVO 处理
        return vo;
    }

    /**
     * 卡组详情 + 卡片列表（完整版，供 Controller 使用）
     */
    public DeckDetailResult getDeckDetail(Long userId, Long deckId) {
        FlashcardDeck deck = requireDeck(deckId, userId);

        List<Flashcard> cards = flashcardMapper.selectList(
                new LambdaQueryWrapper<Flashcard>()
                        .eq(Flashcard::getDeckId, deckId)
                        .orderByAsc(Flashcard::getSortOrder));

        FlashcardDeckVO deckVO = new FlashcardDeckVO();
        deckVO.setId(deck.getId());
        deckVO.setNoteId(deck.getNoteId());
        deckVO.setTitle(deck.getTitle());
        deckVO.setCardCount(deck.getCardCount());
        deckVO.setCreatedAt(deck.getCreatedAt());

        List<FlashcardVO> cardVOs = cards.stream().map(this::toCardVO).collect(Collectors.toList());

        DeckDetailResult result = new DeckDetailResult();
        result.setDeck(deckVO);
        result.setCards(cardVOs);
        return result;
    }

    /**
     * 删除卡组及所有卡片
     *
     * @param userId 当前用户 ID
     * @param deckId 卡组 ID
     */
    @Transactional
    public void deleteDeck(Long userId, Long deckId) {
        requireDeck(deckId, userId);

        // 删除所有卡片
        flashcardMapper.delete(
                new LambdaQueryWrapper<Flashcard>().eq(Flashcard::getDeckId, deckId));

        // 删除卡组
        deckMapper.deleteById(deckId);
        log.info("删除卡组，userId={}, deckId={}", userId, deckId);
    }

    /**
     * 编辑单张卡片（front/back）
     *
     * @param userId  当前用户 ID
     * @param cardId  卡片 ID
     * @param request 编辑请求
     */
    public void updateCard(Long userId, Long cardId, UpdateCardRequest request) {
        Flashcard card = requireCard(cardId, userId);
        card.setFront(request.getFront());
        card.setBack(request.getBack());
        flashcardMapper.updateById(card);
        log.info("编辑卡片，userId={}, cardId={}", userId, cardId);
    }

    /**
     * 今日待复习卡片（SRS 到期或从未复习）
     *
     * @param userId 当前用户 ID
     */
    public List<FlashcardVO> todayReview(Long userId) {
        List<Flashcard> cards = flashcardMapper.selectTodayReview(userId);
        return cards.stream().map(this::toCardVO).collect(Collectors.toList());
    }

    /**
     * 提交卡片复习结果，更新 SRS 调度字段
     *
     * @param userId  当前用户 ID
     * @param cardId  卡片 ID
     * @param request 复习结果（known: true/false）
     */
    public void submitReview(Long userId, Long cardId, FlashcardReviewRequest request) {
        Flashcard card = requireCard(cardId, userId);
        boolean known = Boolean.TRUE.equals(request.getKnown());

        int currentInterval = card.getReviewIntervalDays() != null
                ? card.getReviewIntervalDays() : 1;
        int nextInterval = srsService.calcNextInterval(known, currentInterval);
        LocalDateTime nextReviewAt = srsService.calcNextReviewAt(nextInterval);

        card.setReviewIntervalDays(nextInterval);
        card.setNextReviewAt(nextReviewAt);
        card.setReviewCount(card.getReviewCount() != null ? card.getReviewCount() + 1 : 1);

        if (known) {
            card.setConsecutiveCorrect(
                    card.getConsecutiveCorrect() != null ? card.getConsecutiveCorrect() + 1 : 1);
        } else {
            card.setConsecutiveCorrect(0);
        }

        flashcardMapper.updateById(card);
        log.info("提交卡片复习，userId={}, cardId={}, known={}, nextInterval={}天",
                userId, cardId, known, nextInterval);
    }

    // ===== 内部数据结构 =====

    /**
     * 卡组详情结果（卡组 VO + 卡片列表）
     */
    @lombok.Data
    public static class DeckDetailResult {
        private FlashcardDeckVO deck;
        private List<FlashcardVO> cards;
    }

    // ===== 私有方法 =====

    private FlashcardDeck requireDeck(Long deckId, Long userId) {
        FlashcardDeck deck = deckMapper.selectById(deckId);
        if (deck == null) {
            throw new BizException(ResultCode.FLASHCARD_DECK_NOT_FOUND);
        }
        if (!deck.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return deck;
    }

    private Flashcard requireCard(Long cardId, Long userId) {
        Flashcard card = flashcardMapper.selectById(cardId);
        if (card == null) {
            throw new BizException(ResultCode.FLASHCARD_NOT_FOUND);
        }
        if (!card.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        return card;
    }

    private FlashcardVO toCardVO(Flashcard card) {
        FlashcardVO vo = new FlashcardVO();
        vo.setId(card.getId());
        vo.setDeckId(card.getDeckId());
        vo.setFront(card.getFront());
        vo.setBack(card.getBack());
        vo.setSortOrder(card.getSortOrder());
        vo.setReviewCount(card.getReviewCount());
        vo.setConsecutiveCorrect(card.getConsecutiveCorrect());
        vo.setReviewIntervalDays(card.getReviewIntervalDays());
        vo.setNextReviewAt(card.getNextReviewAt());
        vo.setCreatedAt(card.getCreatedAt());
        return vo;
    }

    private String buildNoteContent(Note note) {
        StringBuilder sb = new StringBuilder();
        if (note.getTitle() != null) {
            sb.append("标题：").append(note.getTitle()).append("\n\n");
        }
        if (note.getContent() != null && !note.getContent().isBlank()) {
            sb.append(note.getContent());
        } else if (note.getOcrText() != null && !note.getOcrText().isBlank()) {
            sb.append(note.getOcrText());
        }
        return sb.toString().trim();
    }

    private String loadPrompt(String filename) {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/" + filename);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("加载 Prompt 模板失败: {}", filename);
            throw new BizException(ResultCode.INTERNAL_ERROR, "Prompt 模板加载失败");
        }
    }
}
