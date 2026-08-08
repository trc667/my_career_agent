package com.example.aimaster.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.dto.AnnouncementRequest;
import com.example.aimaster.dto.KnowledgeRequest;
import com.example.aimaster.dto.PointRequest;
import com.example.aimaster.dto.Result;
import com.example.aimaster.dto.VipRequest;
import com.example.aimaster.entity.Announcement;
import com.example.aimaster.entity.Feedback;
import com.example.aimaster.entity.User;
import com.example.aimaster.exception.BusinessException;
import com.example.aimaster.mapper.AnnouncementMapper;
import com.example.aimaster.mapper.FeedbackMapper;
import com.example.aimaster.mapper.UserMapper;
import com.example.aimaster.service.OssStorageService;
import com.example.aimaster.service.ErrorLogService;
import com.example.aimaster.service.KnowledgeService;
import com.example.aimaster.service.PointService;
import com.example.aimaster.service.AdminStatsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台接口：公告管理、意见反馈查看、用户列表、知识库管理。
 * 全部要求 ROLE_ADMIN（由 SecurityConfig 统一拦截）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AnnouncementMapper announcementMapper;
    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;
    private final OssStorageService ossStorageService;
    private final ErrorLogService errorLogService;
    private final KnowledgeService knowledgeService;
    private final PointService pointService;
    private final AdminStatsService adminStatsService;

    public AdminController(AnnouncementMapper announcementMapper,
                           FeedbackMapper feedbackMapper,
                           UserMapper userMapper,
                           OssStorageService ossStorageService,
                           ErrorLogService errorLogService,
                           KnowledgeService knowledgeService,
                           PointService pointService,
                           AdminStatsService adminStatsService) {
        this.announcementMapper = announcementMapper;
        this.feedbackMapper = feedbackMapper;
        this.userMapper = userMapper;
        this.ossStorageService = ossStorageService;
        this.errorLogService = errorLogService;
        this.knowledgeService = knowledgeService;
        this.pointService = pointService;
        this.adminStatsService = adminStatsService;
    }

    /* ===== 运营看板 ===== */

    /** GET /api/admin/stats 运营总览（用户/活跃/对话/积分/兑换/消费去向） */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(adminStatsService.overview());
    }

    /* ===== 公告管理 ===== */

    @GetMapping("/announcements")
    public Result<List<Announcement>> announcements() {
        return Result.ok(announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>().orderByDesc(Announcement::getId)
        ));
    }

    @PostMapping("/announcements")
    public Result<Void> createAnnouncement(@Valid @RequestBody AnnouncementRequest req) {
        announcementMapper.insert(Announcement.builder()
                .title(req.getTitle().trim())
                .content(req.getContent().trim())
                .createTime(LocalDateTime.now())
                .build());
        return Result.ok("公告已发布", null);
    }

    @PutMapping("/announcements/{id}")
    public Result<Void> updateAnnouncement(@PathVariable Long id,
                                           @Valid @RequestBody AnnouncementRequest req) {
        Announcement ann = announcementMapper.selectById(id);
        if (ann == null) {
            throw new BusinessException("公告不存在");
        }
        ann.setTitle(req.getTitle().trim());
        ann.setContent(req.getContent().trim());
        announcementMapper.updateById(ann);
        return Result.ok("公告已更新", null);
    }

    @DeleteMapping("/announcements/{id}")
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementMapper.deleteById(id);
        return Result.ok("公告已删除", null);
    }

    /* ===== 意见反馈 ===== */

    @GetMapping("/feedbacks")
    public Result<List<Feedback>> feedbacks() {
        return Result.ok(feedbackMapper.selectList(
                new LambdaQueryWrapper<Feedback>().orderByDesc(Feedback::getId)
        ));
    }

    @DeleteMapping("/feedbacks/{id}")
    public Result<Void> deleteFeedback(@PathVariable Long id) {
        feedbackMapper.deleteById(id);
        return Result.ok("反馈已删除", null);
    }

    /* ===== 用户列表 ===== */

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> users() {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByAsc(User::getId)
        );
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("role", u.getRole() != null ? u.getRole() : "USER");
            m.put("createTime", u.getCreateTime());
            return m;
        }).toList();
        return Result.ok(result);
    }

    /* ===== AI 头像（全局，所有人可见） ===== */

    /** POST /api/admin/ai-avatar 上传/更换 AI 头像（覆盖固定 key，全体用户可见） */
    @PostMapping("/ai-avatar")
    public Result<Map<String, Object>> uploadAiAvatar(@RequestParam("file") MultipartFile file) {
        String url = ossStorageService.uploadAiAvatar(file);
        Map<String, Object> data = new HashMap<>();
        data.put("avatar", url);
        return Result.ok(data);
    }

    /* ===== 错误日志（自建监控面板） ===== */

    /** GET /api/admin/error-logs 错误日志列表（倒序，可按来源/级别过滤） */
    @GetMapping("/error-logs")
    public Result<List<com.example.aimaster.entity.ErrorLog>> errorLogs(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String level,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        return Result.ok(errorLogService.list(source, level, limit));
    }

    /** DELETE /api/admin/error-logs 清空错误日志 */
    @DeleteMapping("/error-logs")
    public Result<Void> clearErrorLogs() {
        errorLogService.clear();
        return Result.ok("已清空", null);
    }

    /* ===== 知识库管理（RAG 事实源在线增删改查，变更后异步重建索引） ===== */

    /** GET /api/admin/knowledge 分页查询（分类/关键词/启用状态过滤） */
    @GetMapping("/knowledge")
    public Result<KnowledgeService.KnowledgePage> knowledge(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(knowledgeService.list(category, keyword, enabled, page, Math.min(Math.max(size, 1), 100)));
    }

    /** GET /api/admin/knowledge/categories 分类统计 */
    @GetMapping("/knowledge/categories")
    public Result<List<Map<String, Object>>> knowledgeCategories() {
        return Result.ok(knowledgeService.categories());
    }

    /** POST /api/admin/knowledge 新增知识段 */
    @PostMapping("/knowledge")
    public Result<com.example.aimaster.entity.Knowledge> createKnowledge(@Valid @RequestBody KnowledgeRequest req) {
        return Result.ok(knowledgeService.create(req));
    }

    /** PUT /api/admin/knowledge/{id} 更新知识段 */
    @PutMapping("/knowledge/{id}")
    public Result<com.example.aimaster.entity.Knowledge> updateKnowledge(@PathVariable Long id,
                                                                        @Valid @RequestBody KnowledgeRequest req) {
        return Result.ok(knowledgeService.update(id, req));
    }

    /** PUT /api/admin/knowledge/{id}/enabled 启停知识段 */
    @PutMapping("/knowledge/{id}/enabled")
    public Result<Void> toggleKnowledge(@PathVariable Long id,
                                        @RequestParam boolean enabled) {
        knowledgeService.setEnabled(id, enabled);
        return Result.ok("已更新", null);
    }

    /** DELETE /api/admin/knowledge/{id} 删除知识段 */
    @DeleteMapping("/knowledge/{id}")
    public Result<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.ok("已删除", null);
    }

    /** POST /api/admin/knowledge/rebuild 手动触发全量索引重建（异步） */
    @PostMapping("/knowledge/rebuild")
    public Result<Void> rebuildKnowledge() {
        knowledgeService.rebuildAsync();
        return Result.ok("重建已启动", null);
    }

    /** GET /api/admin/knowledge/rebuild-status 重建状态（前端轮询） */
    @GetMapping("/knowledge/rebuild-status")
    public Result<Map<String, Object>> rebuildStatus() {
        return Result.ok(knowledgeService.rebuildStatus());
    }

    /* ===== 积分/会员（商业化起步，管理员手动操作） ===== */

    /** POST /api/admin/points 发放/扣减用户积分（写流水可审计） */
    @PostMapping("/points")
    public Result<Void> changePoints(@Valid @RequestBody PointRequest req) {
        pointService.adminChange(req.getUserId(), req.getDelta(), req.getReason());
        return Result.ok("积分已更新", null);
    }

    /** POST /api/admin/vip 开通/续期 VIP（按天，从当前或现有到期时间起算） */
    @PostMapping("/vip")
    public Result<Void> grantVip(@Valid @RequestBody VipRequest req) {
        pointService.grantVip(req.getUsername(), req.getDays());
        return Result.ok("VIP 已开通", null);
    }
}
