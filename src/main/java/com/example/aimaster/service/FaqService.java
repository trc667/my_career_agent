package com.example.aimaster.service;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * FAQ 精确匹配拦截层：高频通用问题直接命中标准答案，跳过 RAG 检索与 LLM 调用，
 * 省 token 成本、响应更快，且避免模型对产品使用类问题（登录/评分/错题本等）瞎编。
 * <p>
 * 三级匹配策略（归一化后依次尝试）：
 * 1. 精确相等；
 * 2. 问题包含条目关键词（关键词较长，防"你好"这类短词误拦）；
 * 3. 最长公共子串相似度 ≥ 阈值（容忍错别字/口语化表达）。
 * 全部未命中返回 null，走正常 RAG 链路。
 */
@Component
public class FaqService {

    private static final Logger log = LoggerFactory.getLogger(FaqService.class);

    /** 相似度命中阈值（最长公共子串长度 / 关键词长度） */
    private static final double SIMILARITY_THRESHOLD = 0.75;

    /** 参与包含/相似度匹配的关键词最短长度，避免短词误拦 */
    private static final int MIN_KEYWORD_LEN = 3;

    /** FAQ 条目：任一关键词命中即采用该答案 */
    private record FaqEntry(List<String> keywords, String answer) {
    }

    /** 按命中优先级排序：产品使用/账号类高频问题在前 */
    private static final List<FaqEntry> FAQS = List.of(
            new FaqEntry(List.of("这个项目是什么", "这个网站是什么", "你是谁", "介绍一下这个项目", "介绍一下你自己", "这个平台是什么", "这是什么"),
                    "我是一名「计算机学生的一站式求职 AI 助手」，由 Spring Boot + Vue3 打造，核心是用企业级 RAG 技术把 629 段高质量职规知识变成个性化回答。"
                            + "我提供四大场景：① AI 职规大师（RAG 流式对话，回答带【来源】）；② AI 超级智能体（ReAct 多步规划，可联网、查地图、读文件）；"
                            + "③ AI 八股练习场（629 段知识库浏览/搜索/随机抽题/AI 讲解）；④ AI 简历评分（6 维度评分 + 优化版简历）。"
                            + "另有错题本、每日打卡、聊天历史跨设备同步、全站像素风 UI。"
                            + "【来源：产品 FAQ】"),
            new FaqEntry(List.of("怎么登录", "如何登录", "登录不了", "登录失败", "登录不上", "无法登录", "不能登录", "登录没反应",
                    "登入不了", "登不进去", "登不上", "登录不了怎么办", "怎么登不进去"),
                    "登录请点击右上角「登录」按钮，使用注册时的用户名和密码。若提示密码错误可重新注册或核对大小写；"
                            + "若登录没反应，多半是后端服务未启动或网络异常，请稍后刷新重试。"
                            + "注册需要勾选《用户协议》和《隐私政策》。【来源：产品 FAQ】"),
            new FaqEntry(List.of("怎么注册", "如何注册", "注册不了", "注册失败", "注册不了账号", "怎么创建账号"),
                    "点击「注册」填写用户名、邮箱、密码并勾选《用户协议》《隐私政策》即可完成注册，注册成功后自动登录。"
                            + "验证码通过邮箱发送，若收不到请检查垃圾邮件或等待 1 分钟重试。【来源：产品 FAQ】"),
            new FaqEntry(List.of("忘记密码", "密码忘了", "密码错误", "密码不对", "改密码", "重置密码"),
                    "登录页点击「忘记密码」，输入注册时的用户名或邮箱 → 系统向注册邮箱发送 6 位验证码 → 填写验证码和新密码即可重置。验证码 5 分钟内有效，收不到请检查垃圾邮件或 60 秒后重试。【来源：产品 FAQ】"),
            new FaqEntry(List.of("简历评分怎么用", "简历评分", "怎么用简历评分", "简历评估", "简历打分", "帮我评简历"),
                    "进入「AI 简历评分」页面，把你的简历全文粘贴到输入框，点击「开始评分」。"
                            + "系统会结合 RAG 知识库从项目经历、技能栈、量化成果、教育背景、亮点与不足、优化建议 6 个维度打分，"
                            + "并给出优化版简历，结果会自动存入历史可回看。【来源：产品 FAQ】"),
            new FaqEntry(List.of("八股练习场怎么用", "八股练习", "怎么刷题", "八股文怎么练", "练习场怎么用", "怎么练习"),
                    "进入「AI 八股练习场」：① 浏览：按分类查看 629 段知识库；② 搜索：输入关键词定位知识点；"
                            + "③ 随机抽题：随机抽取一道题自测；④ AI 讲解：让 AI 深度讲解当前知识点。"
                            + "答错的题会自动进「错题本」，配合「每日打卡」形成学习闭环。【来源：产品 FAQ】"),
            new FaqEntry(List.of("错题本是什么", "错题本怎么用", "我的错题", "错题怎么查看", "错题本在哪"),
                    "错题本收录你在八股练习中答错或标记不会的题目，可在「错题本与打卡」页查看、练习、移除。"
                            + "搭配每日打卡和连续天数统计，帮你针对性巩固薄弱知识点。【来源：产品 FAQ】"),
            new FaqEntry(List.of("职规大师和超级智能体区别", "超级智能体是什么", "职规大师是什么", "智能体怎么用", "两者区别"),
                    "① AI 职规大师：面向职规咨询的 RAG 问答，回答严谨、带知识来源，适合「校招准备、学习路线、简历优化」等咨询类问题；"
                            + "② AI 超级智能体：具备 ReAct 多步规划能力，可调用高德地图（查附近图书馆/自习室）、联网搜索、PDF 解析、文件保存等工具，"
                            + "适合「帮我查一下、规划一个流程、执行多步任务」这类需要动手的问题。【来源：产品 FAQ】"),
            new FaqEntry(List.of("怎么反馈问题", "怎么提意见", "意见反馈", "反馈渠道", "找客服", "怎么联系你们"),
                    "在页面底部「意见反馈」入口提交，或直接在当前对话中告诉我遇到的问题。"
                            + "后台有自建错误监控，异常会自动上报，你也可以附上操作步骤帮助定位。【来源：产品 FAQ】"),
            new FaqEntry(List.of("收费吗", "怎么收费", "免费吗", "要钱吗", "会员怎么开通", "付费", "收不收费", "花钱吗", "会不会收费"),
                    "当前所有功能免费开放。后续将推出积分/会员体系（用户分级、签到积分、限流分级），"
                            + "届时免费用户仍可正常使用核心功能。【来源：产品 FAQ】"),
            new FaqEntry(List.of("聊天记录会保存吗", "聊天历史", "历史记录", "聊天记录", "跨设备同步", "换设备"),
                    "会保存。聊天会话和消息已全量入库 MySQL，登录同一账号后跨设备自动同步；"
                            + "可新建、切换、删除会话，也能停止生成中的回复。【来源：产品 FAQ】"),
            new FaqEntry(List.of("怎么切换主题", "切换主题", "深色模式", "暗黑模式", "像素风", "主题怎么换"),
                    "页面提供「像素风」主题，可在首页或个人中心切换明暗模式，体验复古像素风格的 UI 动效。【来源：产品 FAQ】"));

    /**
     * 匹配用户问题，命中返回标准答案，未命中返回 null。
     * 匹配基于归一化文本（去标点/空白/转小写），顺序：精确 → 关键词包含 → 相似度。
     */
    public String match(String question) {
        if (question == null || question.isBlank()) return null;
        String q = normalize(question);
        if (q.length() < 2) return null;

        // 1) 精确匹配
        for (FaqEntry e : FAQS) {
            if (e.keywords().stream().anyMatch(k -> normalize(k).equals(q))) {
                log.info("FAQ 精确命中: {}", e.keywords().get(0));
                return e.answer();
            }
        }
        // 2) 关键词包含匹配：问题包含较长关键词
        FaqEntry best = null;
        int bestLen = 0;
        for (FaqEntry e : FAQS) {
            for (String k : e.keywords()) {
                String kn = normalize(k);
                if (kn.length() < MIN_KEYWORD_LEN) continue;
                if (q.contains(kn) && kn.length() > bestLen) {
                    best = e;
                    bestLen = kn.length();
                }
            }
        }
        if (best != null) {
            log.info("FAQ 包含命中: {}", best.keywords().get(0));
            return best.answer();
        }
        // 3) 相似度匹配：最长公共子串 / 关键词长度 ≥ 阈值
        for (FaqEntry e : FAQS) {
            for (String k : e.keywords()) {
                String kn = normalize(k);
                if (kn.length() < MIN_KEYWORD_LEN) continue;
                if (longestCommonSubstringRatio(q, kn) >= SIMILARITY_THRESHOLD) {
                    log.info("FAQ 相似度命中: {}", e.keywords().get(0));
                    return e.answer();
                }
            }
        }
        return null;
    }

    /** 归一化：去掉标点、空白、emoji，转小写（中文无大小写，主要处理英文字母） */
    static String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[\\p{P}\\p{S}\\s]", "").toLowerCase(Locale.ROOT);
    }

    /** 最长公共子串长度与较短串长度之比（0~1） */
    static double longestCommonSubstringRatio(String a, String b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0;
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];
        int maxLen = 0;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    if (dp[i][j] > maxLen) maxLen = dp[i][j];
                }
            }
        }
        int min = Math.min(n, m);
        return min == 0 ? 0 : (double) maxLen / min;
    }
}
