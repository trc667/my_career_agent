/**
 * 积分商城 API：商品列表 / 兑换 / 兑换记录（积分消费出口）
 */
import http from './http';
import type { ResultWrapper } from './chat';

export interface ShopItem {
  id: number;
  name: string;
  description: string;
  points: number;
  type: 'VIP_CARD' | 'CONTENT';
}

export interface RedeemResult {
  itemName: string;
  cost: number;
  type: 'VIP_CARD' | 'CONTENT';
  payload: string;
  pointsLeft: number;
}

export interface RedeemRecord {
  id: number;
  itemId: number;
  itemName: string;
  points: number;
  type: 'VIP_CARD' | 'CONTENT';
  payload: string;
  createTime: string;
}

/** 上架商品列表 */
export function getShopItems() {
  return http.get<any, ResultWrapper<ShopItem[]>>('/api/shop/items');
}

/** 兑换商品（扣积分 + 发放权益） */
export function redeemItem(itemId: number) {
  return http.post<any, ResultWrapper<RedeemResult>>('/api/shop/redeem', { itemId });
}

/** 我的兑换记录 */
export function getRedeemRecords() {
  return http.get<any, ResultWrapper<RedeemRecord[]>>('/api/shop/records');
}
