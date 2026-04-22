package org.example.controller.common;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.constant.Result;
import org.example.mapper.common.LogMapper;
import org.example.pojo.base.Base;
import org.example.pojo.po.common.Log;
import org.example.pojo.base.PageVo;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/log")
public class LogController {

    @Resource
    private LogMapper logMapper;
    
    @Logs("操作列表")
    @Exclude(type = "page", value = {"createBy", "updateBy", "updateTime", "del"})
    @PostMapping("list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<Log> page = new Page<>(req.getInt("currentPage"), req.getInt("pageSize"));
        LambdaQueryWrapper<Log> queryWrapper = new LambdaQueryWrapper<>();

        String keyword = req.getStr("keyword");
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper ->
                    wrapper.like(Log::getOperation, keyword)
                            .or()
                            .like(Log::getUrl, keyword)
            );
        }
        queryWrapper.orderByDesc(Base::getCreateTime);
        logMapper.selectPage(page, queryWrapper);
        PageVo answer = new PageVo(req.getInt("currentPage"), req.getInt("pageSize"));
        answer.setTotal(page.getTotal());
        answer.setData(page.getRecords());
        return Result.success(answer);
    }
}
