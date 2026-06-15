package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.dto.ChatRequestDTO;
import com.sky.entity.*;
import com.sky.mapper.*;
import com.sky.service.ChatService;
import com.sky.service.DishService;
import com.sky.result.Result;
import com.sky.vo.ChatResponseVO;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final String DEEPSEEK_URL = "https://api.deepseek.com/v1/chat/completions";
    private static final String API_KEY = System.getenv("DS_KEY");
    private static final String MODEL = System.getenv().getOrDefault("DS_MODEL", "deepseek-v4-flash");

    @Autowired private DishService dishService;
    @Autowired private SetmealMapper setmealMapper;
    @Autowired private CategoryMapper categoryMapper;

    private static final String SYSTEM_PROMPT = """
        你是"云端食客外卖"的智能客服"小云"。
        职责: 解答菜品咨询，推荐菜品，介绍口味、价格、食材。

        当前菜单:
        %s

        规则:
        1. 热情友好，回复简洁，用emoji
        2. 根据上方菜单回答，不编造菜品
        3. 用户问口味偏好（辣的/清淡/聚会等）→ 从菜单中推荐匹配的
        4. 用户想下单/点菜 → 引导返回首页手动操作
        5. 配送费3元，约2.5km，约10分钟
        6. 愤怒用户 → 先道歉安抚
        """;

    private static final Set<String> NEGATIVE_WORDS = Set.of(
        "烦死了","垃圾","太慢了","投诉","差评","坑","骗","退钱","什么破","无语",
        "操","妈的","傻逼","滚","太差了","受不了");

    @Override
    public Flux<ChatResponseVO> chat(ChatRequestDTO request) {
        return Flux.create(sink -> {
            try {
                String userMsg = request.getMessage();

                // 情感检测
                for (String w : NEGATIVE_WORDS) {
                    if (userMsg.contains(w)) {
                        streamContent("非常抱歉给您带来不好的体验 🙏 需要帮您转接人工客服吗？", sink);
                        sink.next(ChatResponseVO.builder().content("").action("none").done(true).build());
                        sink.complete();
                        return;
                    }
                }

                // 转人工
                if (userMsg.contains("转人工") || userMsg.contains("人工客服") || userMsg.contains("找真人")) {
                    streamContent("正在为您转接人工客服 👩‍💻 预计等待 30 秒……", sink);
                    sink.next(ChatResponseVO.builder().content("").action("none").done(true).build());
                    sink.complete();
                    return;
                }

                // 构建消息：system(含菜单) + history + user
                List<JSONObject> messages = new ArrayList<>();
                JSONObject sys = new JSONObject();
                sys.put("role", "system");
                sys.put("content", String.format(SYSTEM_PROMPT, buildMenuContext()));
                messages.add(sys);

                if (request.getHistory() != null) {
                    for (ChatRequestDTO.ChatMessage h : request.getHistory()) {
                        JSONObject m = new JSONObject();
                        m.put("role", "user".equals(h.getRole()) ? "user" : "assistant");
                        m.put("content", h.getContent());
                        messages.add(m);
                    }
                }

                JSONObject um = new JSONObject();
                um.put("role", "user");
                um.put("content", userMsg);
                messages.add(um);

                // 直接问 DeepSeek，不需要 tools
                JSONObject body = new JSONObject();
                body.put("model", MODEL);
                body.put("messages", messages);
                body.put("stream", false);
                body.put("temperature", 0.7);
                body.put("max_tokens", 1024);

                HttpURLConnection conn = (HttpURLConnection) URI.create(DEEPSEEK_URL).toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
                }

                String resp;
                try (InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
                    resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }

                if (conn.getResponseCode() >= 400) {
                    log.error("DeepSeek API error: {} {}", conn.getResponseCode(), resp);
                    throw new IOException("DeepSeek error: " + resp);
                }

                JSONObject response = JSON.parseObject(resp);
                String content = response.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content");

                if (content != null && !content.isEmpty()) {
                    streamContent(content, sink);
                }

                sink.next(ChatResponseVO.builder().content("").action("none").done(true).build());
                sink.complete();

            } catch (Exception e) {
                log.error("Chat error", e);
                String fb = keywordFallback(request.getMessage());
                if (fb != null) streamContent(fb, sink);
                else sink.next(ChatResponseVO.builder().content("系统出了点小问题，请稍后再试～").action("none").done(true).build());
                sink.complete();
            }
        });
    }

    private String buildMenuContext() {
        try {
            StringBuilder sb = new StringBuilder();
            List<Category> cats = categoryMapper.list(1);
            for (Category cat : cats) {
                Dish q = new Dish(); q.setCategoryId(cat.getId()); q.setStatus(1);
                List<DishVO> dishes = dishService.listWithFlavor(q);
                if (dishes != null && !dishes.isEmpty()) {
                    sb.append("【").append(cat.getName()).append("】\n");
                    for (DishVO d : dishes) {
                        sb.append("  ").append(d.getName()).append(" ¥").append(d.getPrice());
                        if (d.getDescription() != null) sb.append(" (").append(d.getDescription()).append(")");
                        if (d.getFlavors() != null && !d.getFlavors().isEmpty())
                            sb.append(" 可选:").append(d.getFlavors().stream().map(DishFlavor::getName).collect(Collectors.joining(",")));
                        sb.append("\n");
                    }
                }
            }
            List<Category> mealCats = categoryMapper.list(2);
            for (Category cat : mealCats) {
                Setmeal q = new Setmeal(); q.setCategoryId(cat.getId()); q.setStatus(1);
                List<Setmeal> meals = setmealMapper.list(q);
                if (meals != null && !meals.isEmpty()) {
                    sb.append("【").append(cat.getName()).append("(套餐)】\n");
                    for (Setmeal m : meals)
                        sb.append("  ").append(m.getName()).append(" ¥").append(m.getPrice()).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) { return "菜单暂不可用"; }
    }

    @Override
    public Result<String> chatSync(ChatRequestDTO request) {
        try {
            String userMsg = request.getMessage();

            for (String w : NEGATIVE_WORDS) {
                if (userMsg.contains(w)) return Result.success("非常抱歉给您带来不好的体验 🙏 需要帮您转接人工客服吗？");
            }
            if (userMsg.contains("转人工") || userMsg.contains("人工客服"))
                return Result.success("正在为您转接人工客服 👩‍💻 预计等待 30 秒……");

            List<JSONObject> messages = new ArrayList<>();
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", String.format(SYSTEM_PROMPT, buildMenuContext()));
            messages.add(sys);

            if (request.getHistory() != null) {
                for (ChatRequestDTO.ChatMessage h : request.getHistory()) {
                    JSONObject m = new JSONObject();
                    m.put("role", "user".equals(h.getRole()) ? "user" : "assistant");
                    m.put("content", h.getContent());
                    messages.add(m);
                }
            }
            JSONObject um = new JSONObject(); um.put("role", "user"); um.put("content", userMsg);
            messages.add(um);

            JSONObject body = new JSONObject();
            body.put("model", MODEL); body.put("messages", messages);
            body.put("stream", false); body.put("temperature", 0.7); body.put("max_tokens", 1024);

            HttpURLConnection conn = (HttpURLConnection) URI.create(DEEPSEEK_URL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000); conn.setReadTimeout(30000);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
            }
            String resp;
            try (InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream()) {
                resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            String content = JSON.parseObject(resp).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content");
            return Result.success(content);
        } catch (Exception e) {
            log.error("Chat error", e);
            String fb = keywordFallback(request.getMessage());
            return Result.success(fb != null ? fb : "系统出了点小问题，请稍后再试～");
        }
    }

    private void streamContent(String text, FluxSink<ChatResponseVO> sink) {
        for (int i = 0; i < text.length(); i++) {
            sink.next(ChatResponseVO.builder().content(text.substring(i, i + 1)).action("none").done(false).build());
            try { Thread.sleep(20); } catch (InterruptedException e) { break; }
        }
    }

    private String keywordFallback(String msg) {
        if (msg.contains("下单") || msg.contains("结算")) return "请返回首页，点击购物车图标手动下单哦 🛒";
        if (msg.contains("配送") || msg.contains("多久")) return "配送约10分钟，配送费3元，距离约2.5km 🛵";
        if (msg.contains("菜") || msg.contains("吃")) return "热门推荐：馋嘴牛蛙、卤鹅、白切鸡、蜀味烤鱼～可以返回首页浏览完整菜单 🍽️";
        return null;
    }
}
