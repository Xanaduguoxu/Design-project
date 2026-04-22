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
import org.example.mapper.MedalMapper;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Medal;
import org.example.utils.ReqUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/medal")
public class MedalController {

    @Resource
    private MedalMapper medalMapper;

    @Logs("添加目标")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Medal medal = new Medal();
        BeanUtil.copyProperties(req, medal);
        return Result.success(medalMapper.insert(medal) > 0);
    }

    @Logs("删除目标")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(medalMapper.deleteById(id) > 0);
    }

    @Logs("编辑目标")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Medal medal = new Medal();
        BeanUtil.copyProperties(req, medal);
        return Result.success(medalMapper.updateById(medal) > 0);
    }

    @Logs("目标列表")
    @Exclude(type = "page")
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<Medal> page = new Page<>(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        LambdaQueryWrapper<Medal> queryWrapper = new LambdaQueryWrapper<>();
        //  关键字检索
        String keyword = ReqUtils.getKeyword(req);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Medal::getName, keyword)
                    .or()
                    .like(Medal::getBrief, keyword)
            );
        }

        //  权限判断
        String role = SecurityUtils.getRole();
        if (!"root".equals(role)) {
            queryWrapper.eq(Medal::getCreateBy, SecurityUtils.getUsername());
        }

        queryWrapper.orderByDesc(Medal::getCreateTime);
        medalMapper.selectPage(page, queryWrapper);
        return Result.success(new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req), page.getTotal(), page.getRecords()));
    }

}
