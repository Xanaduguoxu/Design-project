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
import org.example.mapper.ExamMapper;
import org.example.mapper.MedalLogMapper;
import org.example.mapper.UserMapper;
import org.example.pojo.base.Base;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Exam;
import org.example.pojo.po.MedalLog;
import org.example.utils.ReqUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/exam")
public class ExamController {

    @Resource
    private ExamMapper examMapper;

    @Logs("添加考试")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Exam exam = new Exam();
        BeanUtil.copyProperties(req, exam);
        return Result.success(examMapper.insert(exam) > 0);
    }

    @Logs("删除考试")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(examMapper.deleteById(id) > 0);
    }

    @Logs("更新考试")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Exam exam = new Exam();
        BeanUtil.copyProperties(req, exam);
        return Result.success(examMapper.updateById(exam) > 0);
    }

    @Logs("查询考试")
    @Exclude(type = "page", value = {"createTime", "updateBy", "updateTime", "del"})
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<Exam> page = new Page<>(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        LambdaQueryWrapper<Exam> queryWrapper = new LambdaQueryWrapper<>();
        String keyword = ReqUtils.getKeyword(req);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(Exam::getName, keyword));
        }
        queryWrapper.orderByDesc(Exam::getCreateTime);

        queryWrapper.eq(!"root".equals(SecurityUtils.getRole()), Base::getCreateBy, SecurityUtils.getUsername());

        examMapper.selectPage(page, queryWrapper);
        PageVo answer = new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        answer.setTotal(page.getTotal());
        answer.setData(page.getRecords());
        return Result.success(answer);
    }
}