package com.docuagent.docuagent.ai.retrieval;

import com.docuagent.docuagent.vo.RetrievalCandidate;
import com.docuagent.docuagent.vo.RetrievalHitVO;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 单机小资料库的关键词打分与 RRF 排名融合，不依赖数据库中文分词插件。 */
public final class RetrievalRanking {
    private static final Pattern TERMS = Pattern.compile("[a-z][a-z0-9_]*(?:[+*?])?|[\\p{IsHan}]+");
    private static final Set<String> STOP = Set.of("什么", "意思", "如何", "为什么", "哪些", "区别", "之间", "分别", "中的", "里的", "请问", "是否", "介绍", "一下");
    private RetrievalRanking() { }

    /** 保留 r+ 等短术语，中文使用相邻双字；标题命中权重高于正文，重复出现次数封顶。 */
    public static List<RetrievalHitVO> keyword(String query, List<RetrievalCandidate> candidates, int limit) {
        Set<String> terms = terms(query);
        List<RetrievalHitVO> hits = new ArrayList<>();
        for (RetrievalCandidate candidate : candidates) {
            String title = candidate.documentName().toLowerCase(Locale.ROOT);
            String body = candidate.content().toLowerCase(Locale.ROOT);
            double score = 0;
            for (String term : terms) {
                int count = occurrences(body, term);
                // 最多奖励三次正文命中，避免长段落依靠重复刷高分。
                score += (title.contains(term) ? 2.0 : 0) + Math.log1p(Math.min(count, 3));
            }
            if (score > 0) {
                double normalized = score / (terms.size() * (2 + Math.log(4)));
                hits.add(new RetrievalHitVO(candidate.chunkId(), candidate.documentId(), candidate.documentName(),
                        normalized, candidate.content(), "keyword_coverage", null));
            }
        }
        hits.sort(order());
        return new ArrayList<>(hits.subList(0, Math.min(limit, hits.size())));
    }

    /** 按 chunkId 合并候选；相同文档的不同分块不能误去重。 */
    public static List<RetrievalHitVO> fuse(List<RetrievalHitVO> vector, List<RetrievalHitVO> keyword, int k, int limit) {
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, RetrievalHitVO> candidates = new HashMap<>();
        for (List<RetrievalHitVO> channel : List.of(vector, keyword)) {
            Set<Long> seen = new HashSet<>();
            int rank = 0;
            for (RetrievalHitVO hit : channel) {
                // 同一路重复 ID 只计一次，不让重复资料改变排名贡献。
                if (!seen.add(hit.chunkId())) continue;
                rank++;
                scores.merge(hit.chunkId(), 1.0 / (k + rank), Double::sum);
                candidates.putIfAbsent(hit.chunkId(), hit);
            }
        }
        List<RetrievalHitVO> result = new ArrayList<>();
        for (var entry : scores.entrySet()) {
            RetrievalHitVO hit = candidates.get(entry.getKey());
            result.add(new RetrievalHitVO(hit.chunkId(), hit.documentId(), hit.documentName(), entry.getValue(),
                    hit.content(), "rrf", null));
        }
        result.sort(order());
        return new ArrayList<>(result.subList(0, Math.min(limit, result.size())));
    }

    public static Comparator<RetrievalHitVO> order() {
        return Comparator.comparingDouble((RetrievalHitVO hit) -> hit.score()).reversed().thenComparing(RetrievalHitVO::chunkId);
    }

    private static Set<String> terms(String query) {
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = TERMS.matcher(query.toLowerCase(Locale.ROOT));
        while (matcher.find() && result.size() < 64) {
            String token = matcher.group();
            if (token.codePoints().allMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)) {
                if (token.length() == 1) result.add(token);
                for (int i = 0; i + 1 < token.length() && result.size() < 64; i++) {
                    String term = token.substring(i, i + 2);
                    if (!STOP.contains(term)) result.add(term);
                }
            } else result.add(token);
        }
        return result;
    }

    private static int occurrences(String content, String term) {
        int count = 0, index = 0;
        while (count < 3 && (index = content.indexOf(term, index)) >= 0) {
            count++;
            index += term.length();
        }
        return count;
    }
}
