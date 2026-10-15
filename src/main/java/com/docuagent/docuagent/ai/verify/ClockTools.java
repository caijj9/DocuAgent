package com.docuagent.docuagent.ai.verify;

import org.springframework.ai.tool.annotation.Tool;

import java.time.LocalDateTime;

/**
 * Day 0 验证用工具：给模型提供一个"可以被调用"的 Java 方法。
 * <p>
 * 注意：这不是业务代码，只用于验证「工具调用」链路是否打通。
 */
public class ClockTools {

    /**
     * 查询指定城市的当前时间。
     * <p>
     * {@code @Tool} 的 description 是写给模型看的提示词，模型据此判断何时该调用本方法。
     *
     * @param city 城市名，由模型从用户问题中提取
     * @return 该城市的当前时间文本
     */
    @Tool(description = "查询指定城市的当前时间")
    public String currentTime(String city) {
        // 这行 println 是「Java 方法真的被执行过」的唯一证据，不要删
        System.out.println(">>> [Java 方法真的执行了] currentTime(city=" + city + ")");
        return city + " 当前时间：" + LocalDateTime.now();
    }
}
