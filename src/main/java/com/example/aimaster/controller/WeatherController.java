package com.example.aimaster.controller;

import java.util.Map;

import com.example.aimaster.dto.Result;
import com.example.aimaster.service.WeatherService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 天气接口（首页天气面板）：GET /api/weather?lat=&lon= 或 ?city=北京。
 * 数据来自 Open-Meteo 免费接口（后端代理，不暴露任何 key）。
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /** GET /api/weather?lat=39.9&lon=116.4 当前位置天气 */
    @GetMapping
    public Result<Map<String, Object>> weather(
            @RequestParam(required = false, defaultValue = "0") double lat,
            @RequestParam(required = false, defaultValue = "0") double lon,
            @RequestParam(required = false) String city) {
        if (city != null && !city.isBlank()) {
            return Result.ok(weatherService.byCity(city.trim()));
        }
        return Result.ok(weatherService.byLatLon(lat, lon));
    }
}
