package com.sky.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sky.entity.*;
import com.sky.mapper.*;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class MenuVectorService {

    // 使用本地 bigram-hash 向量，无需外部 Embedding API

    @Autowired private DishService dishService;
    @Autowired private DishMapper dishMapper;
    @Autowired private SetmealMapper setmealMapper;
    @Autowired private CategoryMapper categoryMapper;
    @Autowired private RedissonClient redissonClient;

    private boolean ready = false;

    @PostConstruct
    public void init() {
        try {
            buildIndex();
            ready = true;
            log.info("✅ RAG 向量索引初始化完成, 共 {} 条", countVectors());
        } catch (Exception e) {
            log.error("❌ RAG 初始化失败: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void buildIndex() throws Exception {
        // 收集所有菜品+套餐
        List<Map<String, Object>> items = new ArrayList<>();
        List<Category> cats = categoryMapper.list(1);
        for (Category cat : cats) {
            Dish q = new Dish(); q.setCategoryId(cat.getId()); q.setStatus(1);
            List<DishVO> dishes = dishService.listWithFlavor(q);
            if (dishes != null) for (DishVO d : dishes) {
                items.add(Map.of("name", d.getName(), "price", d.getPrice(),
                    "type", "单品", "cat", cat.getName(),
                    "desc", d.getDescription() != null ? d.getDescription() : ""));
            }
        }
        List<Category> mealCats = categoryMapper.list(2);
        for (Category cat : mealCats) {
            Setmeal q = new Setmeal(); q.setCategoryId(cat.getId()); q.setStatus(1);
            List<Setmeal> meals = setmealMapper.list(q);
            if (meals != null) for (Setmeal m : meals) {
                items.add(Map.of("name", m.getName(), "price", m.getPrice(),
                    "type", "套餐", "cat", cat.getName(),
                    "desc", m.getDescription() != null ? m.getDescription() : ""));
            }
        }

        log.info("开始向量化 {} 道菜品...", items.size());

        // 逐条生成 embedding 并写入 Redis
        for (Map<String, Object> item : items) {
            String text = item.get("name") + "。" + item.get("desc") + "。" + item.get("cat");
            float[] vec = getEmbedding(text);
            if (vec == null) continue;

            // 存为 Redis hash: menu:{name}
            String key = "menu:" + item.get("name");
            Map<String, String> fields = new HashMap<>();
            fields.put("name", (String) item.get("name"));
            fields.put("price", item.get("price").toString());
            fields.put("type", (String) item.get("type"));
            fields.put("category", (String) item.get("cat"));
            fields.put("desc", (String) item.get("desc"));
            fields.put("embedding", floatArrayToBytes(vec)); // binary blob
            redissonClient.getMap(key).putAll(fields);
        }

        // RAG 使用客户端余弦相似度计算，无需 RediSearch 索引
    }

    private int countVectors() {
        try {
            int count = 0;
            for (String key : redissonClient.getKeys().getKeysByPattern("menu:*")) count++;
            return count;
        } catch (Exception e) { return 0; }
    }

    private float[] getEmbedding(String text) {
        // 本地哈希向量: bigram hash → 1024维归一化向量
        // 不需要外部 API，对中文短文本语义匹配效果可接受
        int dim = 1024;
        float[] vec = new float[dim];
        String s = text.toLowerCase().replaceAll("\\s+", "");
        for (int i = 0; i < s.length() - 1; i++) {
            int h = (s.charAt(i) * 31 + s.charAt(i + 1)) & 0x7FFFFFFF;
            vec[h % dim] += 1.0f;
        }
        // 单字特征
        for (int i = 0; i < s.length(); i++) {
            int h = (s.charAt(i) * 127) & 0x7FFFFFFF;
            vec[h % dim] += 0.5f;
        }
        // L2 归一化
        float norm = 0;
        for (float v : vec) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < dim; i++) vec[i] /= norm;
        return vec;
    }

    private String floatArrayToBytes(float[] vec) {
        byte[] bytes = new byte[vec.length * 4];
        for (int i = 0; i < vec.length; i++) {
            int bits = Float.floatToIntBits(vec[i]);
            bytes[i * 4] = (byte) (bits >> 24);
            bytes[i * 4 + 1] = (byte) (bits >> 16);
            bytes[i * 4 + 2] = (byte) (bits >> 8);
            bytes[i * 4 + 3] = (byte) bits;
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * RAG 搜索 — 向量语义匹配
     */
    public List<Map<String, Object>> search(String query, int topK) {
        if (!ready) return Collections.emptyList();

        float[] queryVec = getEmbedding(query);
        if (queryVec == null) return Collections.emptyList();

        String queryBlob = floatArrayToBytes(queryVec);
        String q = "*=>[KNN " + topK + " @embedding $vec AS score]";
        // 用 Redis SET + FT.SEARCH
        try {
            // 简化处理: 暴力遍历匹配
            List<Map<String, Object>> results = new ArrayList<>();
            for (String key : redissonClient.getKeys().getKeysByPattern("menu:*")) {
                Map<Object, Object> raw = redissonClient.getMap(key).readAllMap();
                if (raw == null) continue;
                Map<String, String> fields = new HashMap<>();
                raw.forEach((k, v) -> fields.put(String.valueOf(k), String.valueOf(v)));
                if (fields == null || !fields.containsKey("embedding")) continue;
                float[] stored = bytesToFloatArray(fields.get("embedding"));
                float score = cosineSimilarity(queryVec, stored);
                if (score > 0.5f) {
                    Map<String, Object> r = new HashMap<>();
                    r.put("name", fields.get("name"));
                    r.put("price", new BigDecimal(fields.get("price")));
                    r.put("type", fields.get("type"));
                    r.put("category", fields.get("category"));
                    r.put("score", score);
                    results.add(r);
                }
            }
            results.sort((a, b) -> Float.compare((float) b.get("score"), (float) a.get("score")));
            return results.subList(0, Math.min(topK, results.size()));
        } catch (Exception e) {
            log.warn("RAG search error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private float[] bytesToFloatArray(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        float[] vec = new float[bytes.length / 4];
        for (int i = 0; i < vec.length; i++) {
            int bits = (bytes[i * 4] & 0xFF) << 24 | (bytes[i * 4 + 1] & 0xFF) << 16
                     | (bytes[i * 4 + 2] & 0xFF) << 8 | (bytes[i * 4 + 3] & 0xFF);
            vec[i] = Float.intBitsToFloat(bits);
        }
        return vec;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length && i < b.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-8));
    }

    public boolean isReady() { return ready; }
}
