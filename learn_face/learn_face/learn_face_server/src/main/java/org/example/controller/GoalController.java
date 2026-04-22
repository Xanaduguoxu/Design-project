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
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Goal;
import org.example.utils.ReqUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/goal")
public class GoalController {

    @Resource
    private GoalMapper goalMapper;

    @Logs("添加目标")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Goal goal = new Goal();
        BeanUtil.copyProperties(req, goal);
        //In progress, completed, give_up
        goal.setStatus("progress");
        goal.setUserId(SecurityUtils.getUserId());
        goal.setCurrentValue(0);
        return Result.success(goalMapper.insert(goal) > 0);
    }

    @Logs("删除目标")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        Goal goal = goalMapper.selectById(id);
        if (goal == null) {
            return Result.fail(false);
        }
        String role = SecurityUtils.getRole();
        if (!"root".equals(role) && !Objects.equals(goal.getCreateBy(), SecurityUtils.getUsername())) {
            return Result.fail(false);
        }
        return Result.success(goalMapper.deleteById(id) > 0);
    }

    @Logs("编辑目标")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Goal goal = new Goal();
        BeanUtil.copyProperties(req, goal);
        Integer targetValue = goal.getTargetValue();
        Integer currentValue = req.getInt("currentValue");
        if (Objects.equals(currentValue, targetValue)) {
            goal.setStatus("completed");
        }
        return Result.success(goalMapper.updateById(goal) > 0);
    }

    @Logs("目标列表")
    @Exclude(type = "page")
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<Goal> page = new Page<>(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        LambdaQueryWrapper<Goal> queryWrapper = new LambdaQueryWrapper<>();
        //  关键字检索
        String keyword = ReqUtils.getKeyword(req);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Goal::getTitle, keyword)
                    .or()
                    .like(Goal::getDescription, keyword)
            );
        }

        //  字典类型检索
        queryWrapper.eq(StrUtil.isNotBlank(ReqUtils.getStatus(req)), Goal::getStatus, ReqUtils.getStatus(req));

        //  权限判断
        String role = SecurityUtils.getRole();
        if (!"root".equals(role)) {
            queryWrapper.eq(Goal::getCreateBy, SecurityUtils.getUsername());
        }

        queryWrapper.orderByDesc(Goal::getCreateTime);
        goalMapper.selectPage(page, queryWrapper);
        return Result.success(new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req), page.getTotal(), page.getRecords()));
    }

    @Logs("id映射")
    @GetMapping("/mapId")
    public Result<List<JSONObject>> mapId() {

        List<JSONObject> collect = goalMapper.selectList(null).stream().map(item -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("id", item.getId());
            jsonObject.putOpt("name", item.getTitle());
            return jsonObject;
        }).collect(Collectors.toList());
        return Result.success(collect);
    }
}
