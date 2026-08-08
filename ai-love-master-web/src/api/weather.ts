/**
 * 天气 API（首页天气面板，后端代理 Open-Meteo）
 */
import http from './http';
import type { ResultWrapper } from './chat';

export type WeatherEffect = 'sunny' | 'partly' | 'cloudy' | 'fog' | 'drizzle' | 'rain' | 'snow' | 'thunder';

export interface WeatherData {
  city: string;
  temp: number;
  weatherCode: number;
  description: string;
  effect: WeatherEffect;
  windSpeed: number;
  updatedAt: string;
}

/** 按城市或经纬度查询实时天气 */
export function getWeather(city?: string, lat?: number, lon?: number) {
  const params: Record<string, string | number> = {};
  if (city) params.city = city;
  if (lat) params.lat = lat;
  if (lon) params.lon = lon;
  return http.get<any, ResultWrapper<WeatherData>>('/api/weather', { params });
}
