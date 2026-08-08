package com.example.aimaster.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 天气服务：代理 Open-Meteo（免费、无需 key），支持经纬度或城市名。
 * <p>
 * 前端天气面板数据源：WMO 天气代码 → 中文描述 + 动效类型（sunny/rain/snow…），
 * 前端据 effect 渲染对应天气动画（雨滴/雪花/阳光…）。
 */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String GEO_URL = "https://geocoding-api.open-meteo.com/v1/search";

    /** 常用城市 → 经纬度（前端城市 chip 一致；避免每次地理编码、降低第三方依赖） */
    private static final Map<String, double[]> CITY_LL = Map.ofEntries(
            Map.entry("北京", new double[] { 39.9042, 116.4074 }),
            Map.entry("上海", new double[] { 31.2304, 121.4737 }),
            Map.entry("广州", new double[] { 23.1291, 113.2644 }),
            Map.entry("深圳", new double[] { 22.5431, 114.0579 }),
            Map.entry("成都", new double[] { 30.5728, 104.0668 }),
            Map.entry("杭州", new double[] { 30.2741, 120.1551 }),
            Map.entry("武汉", new double[] { 30.5928, 114.3055 }),
            Map.entry("西安", new double[] { 34.3416, 108.9398 }),
            Map.entry("南京", new double[] { 32.0603, 118.7969 }),
            Map.entry("重庆", new double[] { 29.5630, 106.5516 }));

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    public WeatherService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 按经纬度查询实时天气（lat/lon 为 0 时兜底北京） */
    public Map<String, Object> byLatLon(double lat, double lon) {
        double useLat = lat == 0 ? 39.9042 : lat;
        double useLon = lon == 0 ? 116.4074 : lon;
        String url = FORECAST_URL + "?latitude=" + useLat + "&longitude=" + useLon
                + "&current=temperature_2m,weather_code,wind_speed_10m";
        return parse(url, "当前位置");
    }

    /** 按城市名查询：先查内置城市表，未命中再走 Open-Meteo 地理编码，仍失败报错 */
    public Map<String, Object> byCity(String city) {
        double[] ll = CITY_LL.get(city);
        if (ll != null) {
            String url = FORECAST_URL + "?latitude=" + ll[0] + "&longitude=" + ll[1]
                    + "&current=temperature_2m,weather_code,wind_speed_10m";
            return parse(url, city);
        }
        String geoUrl = GEO_URL + "?name=" + java.net.URLEncoder.encode(city, java.nio.charset.StandardCharsets.UTF_8)
                + "&count=1&language=zh";
        try {
            JsonNode geo = objectMapper.readTree(restClient.get().uri(java.net.URI.create(geoUrl)).retrieve().body(String.class));
            JsonNode results = geo.path("results");
            if (results.isArray() && results.size() > 0) {
                double lat = results.get(0).path("latitude").asDouble();
                double lon = results.get(0).path("longitude").asDouble();
                String name = results.get(0).path("name").asText(city);
                String url = FORECAST_URL + "?latitude=" + lat + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code,wind_speed_10m";
                return parse(url, name);
            }
        } catch (Exception e) {
            log.warn("天气-城市解析失败: city={} err={}", city, e.getMessage());
        }
        throw new com.example.aimaster.exception.BusinessException("未找到该城市，试试「北京」「上海」等常用城市");
    }

    private Map<String, Object> parse(String url, String city) {
        try {
            String body = restClient.get().uri(java.net.URI.create(url)).retrieve().body(String.class);
            JsonNode cur = objectMapper.readTree(body).path("current");
            int code = cur.path("weather_code").asInt(0);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("city", city);
            m.put("temp", cur.path("temperature_2m").asDouble());
            m.put("weatherCode", code);
            m.put("description", describe(code));
            m.put("effect", effectOf(code));
            m.put("windSpeed", cur.path("wind_speed_10m").asDouble());
            m.put("updatedAt", LocalDateTime.now().toString());
            return m;
        } catch (Exception e) {
            log.error("天气接口调用失败: {}", e.getMessage());
            throw new com.example.aimaster.exception.BusinessException("天气服务暂时不可用，请稍后重试");
        }
    }

    /** WMO 天气代码 → 中文描述 */
    private String describe(int code) {
        return switch (code) {
            case 0, 1 -> "晴朗";
            case 2 -> "多云";
            case 3 -> "阴天";
            case 45, 48 -> "有雾";
            case 51, 53, 55 -> "毛毛雨";
            case 56, 57 -> "冻毛毛雨";
            case 61 -> "小雨";
            case 63 -> "中雨";
            case 65 -> "大雨";
            case 66, 67 -> "冻雨";
            case 71 -> "小雪";
            case 73 -> "中雪";
            case 75, 77 -> "大雪";
            case 80, 81, 82 -> "阵雨";
            case 85, 86 -> "阵雪";
            case 95 -> "雷阵雨";
            case 96, 99 -> "雷暴伴冰雹";
            default -> "天气未知";
        };
    }

    /** WMO 天气代码 → 前端动效类型（sunny/cloudy/rain/snow/fog/thunder…） */
    private String effectOf(int code) {
        if (code == 0 || code == 1) return "sunny";
        if (code == 2) return "partly";
        if (code == 3) return "cloudy";
        if (code == 45 || code == 48) return "fog";
        if (code == 51 || code == 53 || code == 55 || code == 56 || code == 57) return "drizzle";
        if (code == 61 || code == 63 || code == 65 || code == 66 || code == 67 || code == 80 || code == 81 || code == 82) return "rain";
        if (code == 71 || code == 73 || code == 75 || code == 77 || code == 85 || code == 86) return "snow";
        if (code == 95 || code == 96 || code == 99) return "thunder";
        return "cloudy";
    }
}
