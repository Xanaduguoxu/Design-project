package org.example.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.config.security.LoginUser;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.mapper.ForumMapper;
import org.example.mapper.UserMapper;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Forum;
import org.example.pojo.po.User;
import org.example.utils.ReqUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/forum")
public class ForumController {

    @Resource
    private ForumMapper forumMapper;

    @Resource
    private UserMapper userMapper;

    @Logs("添加论坛")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Forum forum = new Forum();
        BeanUtil.copyProperties(req, forum);
        forum.setLikes(0);
        forum.setViews(0);
        User loginUser = SecurityUtils.getUser();
        forum.setAuthor(loginUser.getNickname());
        forum.setAvatar(loginUser.getAvatar());
        return Result.success(forumMapper.insert(forum) > 0);
    }

    @Logs("删除论坛")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(forumMapper.deleteById(id) > 0);
    }

    @Logs("更新论坛")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Forum forum = new Forum();
        BeanUtil.copyProperties(req, forum);
        return Result.success(forumMapper.updateById(forum) > 0);
    }

    @Logs("查询论坛")
    @Exclude(type = "page")
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<Forum> page = new Page<>(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        LambdaQueryWrapper<Forum> queryWrapper = new LambdaQueryWrapper<>();
        String keyword = ReqUtils.getKeyword(req);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Forum::getAuthor, keyword)
                    .or()
                    .like(Forum::getBio, keyword)
                    .or()
                    .like(Forum::getTitle, keyword)
            );
        }
        queryWrapper.orderByDesc(Forum::getCreateTime);
        forumMapper.selectPage(page, queryWrapper);
        PageVo answer = new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        answer.setTotal(page.getTotal());
        answer.setData(page.getRecords());
        return Result.success(answer);
    }

    @Logs("论坛详情")
    @Exclude(type = "details")
    @GetMapping("/details")
    public Result<JSONObject> details(@RequestParam("id") Long id) {
        return Result.success(new JSONObject(forumMapper.selectById(id)));
    }

    @Logs("热门")
    @Exclude(type = "list")
    @GetMapping("/top")
    public Result<List<JSONObject>> top() {
        List<Forum> forums = forumMapper.selectList(null);
        Collections.shuffle(forums);
        List<JSONObject> collect = forums.stream().map(item -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("id", item.getId().toString());
            jsonObject.putOpt("title", item.getTitle());
            jsonObject.putOpt("bio", item.getBio());
            return jsonObject;
        }).collect(Collectors.toList());
        return Result.success(collect);
    }
}