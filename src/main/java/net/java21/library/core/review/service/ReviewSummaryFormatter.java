package net.java21.library.core.review.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM이 생성한 리뷰 요약을 정리하고 화면 표시용 항목으로 나눕니다.
 *
 * <p>LLM은 지시와 달리 Markdown 기호(**, #), 인사말("물론입니다! ~ 드리겠습니다."),
 * "최종 요약" 같은 제목 줄을 덧붙이곤 합니다. 저장 전과 표시 전에 이를 제거하고,
 * [장점], [단점], [추천 대상], [총평] 항목으로 나눕니다.</p>
 */
public final class ReviewSummaryFormatter {

    /** 요약 항목 순서 (프롬프트의 출력 형식과 일치) */
    public static final List<String> SECTION_LABELS = List.of("장점", "단점", "추천 대상", "총평");

    private static final Pattern MARKDOWN_EMPHASIS = Pattern.compile("\\*\\*|__|`");
    private static final Pattern LEADING_MARKDOWN = Pattern.compile("^\\s*(#{1,6}\\s*|[-*•]\\s+|>\\s*)");
    private static final Pattern TITLE_LINE = Pattern.compile("^\\s*\\[?(최종\\s*)?(평판\\s*)?요약\\]?\\s*:?\\s*$");
    private static final Pattern PREAMBLE_LINE = Pattern.compile(
            "^\\s*(물론|네[,!. ]|알겠습니다|좋습니다|다음은|아래는).*(드리겠습니다|드립니다|정리했습니다|입니다)[.!]?\\s*$");
    /** [장점], 장점:, [추천대상]: 처럼 다양한 항목 머리말을 인식 */
    private static final Pattern SECTION_HEAD = Pattern.compile(
            "^\\s*\\[?\\s*(장점|단점|추천\\s*대상|총평)\\s*\\]?\\s*[:：]?\\s*(.*)$");

    private ReviewSummaryFormatter() {
    }

    /**
     * 요약 항목 하나 (라벨과 본문)
     */
    public record Section(String label, String text) {
    }

    /**
     * Markdown 기호, 인사말, 제목 줄을 제거한 요약 텍스트를 반환합니다.
     *
     * @param raw LLM 원문
     * @return 정리된 텍스트 (null이면 null)
     */
    public static String clean(String raw) {
        if (raw == null) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        for (String line : raw.strip().split("\\R")) {
            String cleaned = LEADING_MARKDOWN.matcher(MARKDOWN_EMPHASIS.matcher(line).replaceAll("")).replaceFirst("").strip();
            if (TITLE_LINE.matcher(cleaned).matches() || PREAMBLE_LINE.matcher(cleaned).matches()) {
                continue;
            }
            if (cleaned.isEmpty() && (lines.isEmpty() || lines.get(lines.size() - 1).isEmpty())) {
                continue;
            }
            lines.add(cleaned);
        }
        return String.join("\n", lines).strip();
    }

    /**
     * 정리한 요약을 [장점], [단점], [추천 대상], [총평] 항목으로 나눕니다.
     *
     * <p>항목 머리말이 하나도 없으면 빈 목록을 반환하므로, 호출 측은 {@link #clean(String)} 결과를
     * 그대로 보여 주면 됩니다. 항목 앞의 문장(머리말 없이 쓴 개요)은 "요약" 항목으로 담습니다.</p>
     *
     * @param raw LLM 원문 또는 저장된 요약
     * @return 표시 순서대로 정렬된 항목 목록
     */
    public static List<Section> parse(String raw) {
        String cleaned = clean(raw);
        if (cleaned == null || cleaned.isEmpty()) {
            return List.of();
        }

        List<String> labels = new ArrayList<>();
        List<StringBuilder> bodies = new ArrayList<>();
        StringBuilder intro = new StringBuilder();

        for (String line : cleaned.split("\\R")) {
            Matcher head = SECTION_HEAD.matcher(line);
            if (head.matches()) {
                String label = head.group(1).replaceAll("\\s+", "").equals("추천대상") ? "추천 대상" : head.group(1);
                int existing = labels.indexOf(label);
                if (existing >= 0) {
                    appendLine(bodies.get(existing), head.group(2));
                    continue;
                }
                labels.add(label);
                bodies.add(new StringBuilder(head.group(2).strip()));
            } else if (!labels.isEmpty()) {
                appendLine(bodies.get(bodies.size() - 1), line);
            } else {
                appendLine(intro, line);
            }
        }

        if (labels.isEmpty()) {
            return List.of();
        }

        List<Section> sections = new ArrayList<>();
        if (!intro.isEmpty()) {
            sections.add(new Section("요약", intro.toString().strip()));
        }
        for (String label : SECTION_LABELS) {
            int index = labels.indexOf(label);
            if (index >= 0 && !bodies.get(index).isEmpty()) {
                sections.add(new Section(label, bodies.get(index).toString().strip()));
            }
        }
        return sections;
    }

    private static void appendLine(StringBuilder target, String line) {
        String text = line.strip();
        if (text.isEmpty()) {
            return;
        }
        if (!target.isEmpty()) {
            target.append(' ');
        }
        target.append(text);
    }
}
