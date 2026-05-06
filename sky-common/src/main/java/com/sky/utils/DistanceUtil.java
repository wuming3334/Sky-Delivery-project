package com.sky.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 百度地图距离计算工具类
 */
@Component
public class DistanceUtil {
    @Value("${sky.baidu.ak}")
    private String akValue;

    public static String ak;

    @PostConstruct
    public void init() {
        ak = this.akValue;
    }
    private static final String GEOCODING_URL = "https://api.map.baidu.com/geocoding/v3/?";
    private static final String DISTANCE_URL = "https://api.map.baidu.com/routematrix/v2/driving?";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据地址获取经纬度坐标
     */
    public static double[] getCoordinates(String address) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("address", address);
        params.put("output", "json");
        params.put("ak", ak);

        String result = requestGet(GEOCODING_URL, params);
        JsonNode jsonNode = objectMapper.readTree(result);

        if (jsonNode.get("status").asInt() == 0) {
            JsonNode location = jsonNode.get("result").get("location");
            double lng = location.get("lng").asDouble();
            double lat = location.get("lat").asDouble();
            return new double[]{lng, lat};
        }
        throw new RuntimeException("地理编码失败: " + jsonNode.get("message").asText());
    }

    /**
     * 计算两个地址之间的距离（米）
     */
    public static double calculateDistance(String origin, String destination) throws Exception {
        double[] originCoords = getCoordinates(origin);
        double[] destCoords = getCoordinates(destination);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("origins", originCoords[1] + "," + originCoords[0]);
        params.put("destinations", destCoords[1] + "," + destCoords[0]);
        params.put("ak", ak);
        params.put("output", "json");

        String result = requestGet(DISTANCE_URL, params);
        JsonNode jsonNode = objectMapper.readTree(result);

        if (jsonNode.get("status").asInt() == 0) {
            JsonNode resultNode = jsonNode.get("result");
            if (resultNode != null && resultNode.size() > 0) {
                JsonNode firstResult = resultNode.get(0);
                JsonNode distance = firstResult.get("distance");
                if (distance != null) {
                    return distance.get("value").asDouble();
                }
            }
        }
        throw new RuntimeException("距离计算失败");
    }

    /**
     * 校验是否在配送范围内
     */
    public static boolean isWithinRange(String userAddress, String restaurantAddress, double maxDistance) {
        try {
            double distance = calculateDistance(userAddress, restaurantAddress);
            return distance <= maxDistance;
        } catch (Exception e) {
            throw new RuntimeException("配送范围校验失败", e);
        }
    }

    private static String requestGet(String strUrl, Map<String, String> param) throws Exception {
        StringBuilder queryString = new StringBuilder(strUrl);
        for (Map.Entry<String, String> entry : param.entrySet()) {
            queryString.append(entry.getKey()).append("=")
                    .append(UriUtils.encode(entry.getValue(), "UTF-8")).append("&");
        }
        if (queryString.charAt(queryString.length() - 1) == '&') {
            queryString.deleteCharAt(queryString.length() - 1);
        }

        URL url = new URL(queryString.toString());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
        StringBuilder buffer = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line);
        }
        reader.close();
        connection.disconnect();
        return buffer.toString();
    }

    /**
     * 规划路线
     * @param origin
     * @param destination
     * @return
     * @throws Exception
     */
    public static String getRoutePlan(String origin, String destination) throws Exception {
        double[] originCoords = getCoordinates(origin);
        double[] destCoords = getCoordinates(destination);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("origin", originCoords[1] + "," + originCoords[0]);
        params.put("destination", destCoords[1] + "," + destCoords[0]);
        params.put("ak", ak);

        String result = requestGet(DISTANCE_URL, params);
        JsonNode jsonNode = objectMapper.readTree(result);

        if (jsonNode.get("status").asInt() == 0) {
            JsonNode route = jsonNode.get("result").get("routes").get(0);
            return route.get("overview_polyline").asText();
        }
        throw new RuntimeException("路线规划失败");
    }

}
