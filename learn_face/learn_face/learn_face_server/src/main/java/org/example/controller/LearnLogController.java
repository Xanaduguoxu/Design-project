package org.example.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.mapper.GoalMapper;
import org.example.mapper.LearnLogMapper;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Goal;
import org.example.pojo.po.LearnLog;
import org.example.utils.ReqUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/learnLog")
public class LearnLogController {

    @Resource
    private LearnLogMapper learnLogMapper;

    @Logs("添加目标")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        LearnLog log = new LearnLog();
        BeanUtil.copyProperties(req, log);
        return Result.success(learnLogMapper.insert(log) > 0);
    }

    @Logs("删除目标")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(learnLogMapper.deleteById(id) > 0);
    }

    @Logs("编辑目标")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        LearnLog log = new LearnLog();
        BeanUtil.copyProperties(req, log);
        return Result.success(learnLogMapper.updateById(log) > 0);
    }

    @Logs("目标列表")
    @Exclude(type = "page")
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<LearnLog> page = new Page<>(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        LambdaQueryWrapper<LearnLog> queryWrapper = new LambdaQueryWrapper<>();
        //  关键字检索
        String keyword = ReqUtils.getKeyword(req);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(LearnLog::getName, keyword)
            );
        }

        //  权限判断
        String role = SecurityUtils.getRole();
        if (!"root".equals(role)) {
            queryWrapper.eq(LearnLog::getCreateBy, SecurityUtils.getUsername());
        }

        queryWrapper.orderByDesc(LearnLog::getCreateTime);
        learnLogMapper.selectPage(page, queryWrapper);
        return Result.success(new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req), page.getTotal(), page.getRecords()));
    }
}
