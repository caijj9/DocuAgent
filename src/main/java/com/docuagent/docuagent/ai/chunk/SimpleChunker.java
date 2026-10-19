package com.docuagent.docuagent.ai.chunk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 简单切块器：按字符滑动窗口切分，相邻块之间保留 overlap 个字符的重叠。
 * <p>
 * 举例（chunkSize=8, overlap=2）：
 * <pre>
 * 原文（20 字）：数据库系统由数据库、数据库管理系统、应用程序和数据库管理员组成
 *
 * 块0  [0, 8)    数据库系统由数据
 * 块1  [6, 14)   由数据库、数据       ← 起点 6 = 8 - 2，往回退 overlap 个字
 * 块2  [12, 20)  管理系统、应用程
 * 块3  [18, 20)  组成                ← 最后一块，长度不足 chunkSize
 * </pre>
 * 那个"往回退 overlap 个字"，是为了让跨边界的句子在两块里都能读完整。
 */
@Component
public class SimpleChunker {

    /** 每块的字符数上限 */
    @Value("${docuagent.chunk.size:500}")
    private int chunkSize;

    /** 相邻块之间的重叠字符数（必须小于 chunkSize，否则每次不前进会死循环） */
    @Value("${docuagent.chunk.overlap:100}")
    private int overlap;

    /**
     * 把一段长文本切成若干块。
     *
     * @param text 原文
     * @return 切好的块，按原文顺序排列；text 为空时返回空列表
     */
    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }
        // 保护：overlap >= chunkSize 时 step <= 0，while 会不前进 → 死循环
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "overlap(" + overlap + ") 必须小于 chunkSize(" + chunkSize + ")");
        }

        int step = chunkSize - overlap;      // 每次窗口前进多少
        int length = text.length();
        int i = 0;

        while (i < length) {
            int end = Math.min(i + chunkSize, length);   // Math.min 防止最后一块越界
            chunks.add(text.substring(i, end));

            if (end == length) {
                break;                            // 已到末尾，别再往前走（否则重复切最后一块）
            }
            i += step;
        }
        return chunks;
    }
}
