package com.example.aimaster.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aimaster.dto.Result;
import com.example.aimaster.entity.Announcement;
import com.example.aimaster.mapper.AnnouncementMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公告接口：最新公告 + 公告列表（访客可见，无需登录）。
 */
@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    private final AnnouncementMapper announcementMapper;

    public AnnouncementController(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    /** GET /api/announcement/latest 最新一条公告（无公告返回 null） */
    @GetMapping("/latest")
    public Result<Announcement> latest() {
        List<Announcement> list = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .orderByDesc(Announcement::getId)
                        .last("LIMIT 1")
        );
        return Result.ok(list.isEmpty() ? null : list.get(0));
    }

    /** GET /api/announcement/list 公告列表（按时间倒序） */
    @GetMapping("/list")
    public Result<List<Announcement>> list() {
        List<Announcement> list = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .orderByDesc(Announcement::getId)
        );
        return Result.ok(list);
    }
}
