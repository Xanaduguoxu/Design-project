package org.example.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.constant.Result;
import org.example.mapper.CourseMapper;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Course;
import org.example.utils.ReqUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/course")
public class CourseController {

    @Resource
    private CourseMapper courseMapper;

    @Logs("添加课程")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Course course = new Course();
        BeanUtil.copyProperties(req, course);
        return Result.success(courseMapper.insert(course) > 0);
    }

    @Logs("删除课程")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(courseMapper.deleteById(id) > 0);
    }

    @Logs("更新课程")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Course course = new Course();
        BeanUtil.copyProperties(req, course);
        return Result.success(courseMapper.updateById(course) > 0);
    }

    @Logs("查询课程")
    @Exclude(type = "page")
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<Course> page = new Page<>(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        LambdaQueryWrapper<Course> queryWrapper = new LambdaQueryWrapper<>();
        String keyword = ReqUtils.getKeyword(req);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(Course::getName, keyword).or().like(Course::getName, keyword).or().like(Course::getCategory, keyword));
        }
        queryWrapper.eq(StrUtil.isNotBlank(ReqUtils.getCategory(req)), Course::getCategory, ReqUtils.getCategory(req));
        queryWrapper.orderByDesc(Course::getCreateTime);
        courseMapper.selectPage(page, queryWrapper);
        PageVo answer = new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        answer.setTotal(page.getTotal());
        answer.setData(page.getRecords());
        return Result.success(answer);
    }

    @Logs("推荐课程")
    @Exclude(type = "list", value = {"createBy", "createTime", "updateBy", "updateTime", "del", "chapters"})
    @GetMapping("/recommend")
    public Result<List<JSONObject>> recommend() {
        List<Course> courses = courseMapper.selectList(null);
        Collections.shuffle(courses);
        return Result.success(courses.stream().limit(8).map(JSONObject::new).collect(Collectors.toList()));
    }

    @Logs("课程详情")
    @Exclude(type = "details")
    @GetMapping("/details")
    public Result<JSONObject> details(@RequestParam("id") Long id) {
        Course course = courseMapper.selectById(id);
        return Result.success(new JSONObject(course));
    }
}
