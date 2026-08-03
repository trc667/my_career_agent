#!/usr/bin/env bash
# 首次克隆：从示例生成 application-dev.yml（该文件在 .gitignore，避免密钥入库）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EX="$ROOT/src/main/resources/application-dev.yml.example"
DEV="$ROOT/src/main/resources/application-dev.yml"
if [[ ! -f "$EX" ]]; then
  echo "找不到: $EX" >&2
  exit 1
fi
if [[ -f "$DEV" ]]; then
  echo "已存在: $DEV — 若需重置请先删除后再运行本脚本。"
  exit 0
fi
cp "$EX" "$DEV"
echo "已生成: $DEV"
echo "请编辑并替换 REPLACE_WITH_*，或设置环境变量 DASHSCOPE_API_KEY、AMAP_MCP_KEY。"
