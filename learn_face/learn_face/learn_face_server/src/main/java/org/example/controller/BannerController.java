package org.example.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.constant.Result;
import org.example.mapper.BannerMapper;
import org.example.pojo.base.Base;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Banner;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/banner")
public class BannerController {


    @Resource
    private BannerMapper bannerMapper;

    @Logs("添加轮播图")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Banner banner = new Banner();
        BeanUtil.copyProperties(req, banner);
        return Result.success(bannerMapper.insert(banner) > 0);
    }

    @Logs("删除轮播图")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(bannerMapper.deleteById(id) > 0);
    }

    @Logs("修改轮播图")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Banner banner = new Banner();
        BeanUtil.copyProperties(req, banner);
        return Result.success(bannerMapper.updateById(banner) > 0);
    }

    @Logs("后台列表")
    @Exclude(type = "page", value = {"del"})
    @PostMapping("/admin/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<Banner> page = new Page<>(req.getInt("currentPage"), req.getInt("pageSize"));
        LambdaQueryWrapper<Banner> queryWrapper = new LambdaQueryWrapper<>();

        String keyword = req.getStr("keyword");
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper ->
                    wrapper.like(Banner::getName, keyword)
                            .or()
                            .like(Banner::getBrief, keyword)
            );
        }
        queryWrapper.orderByAsc(Base::getCreateTime);
        bannerMapper.selectPage(page, queryWrapper);

        List<JSONObject> collect = page.getRecords().stream().map(item -> {
            JSONObject jsonObject = new JSONObject();
            BeanUtil.copyProperties(item, jsonObject);
            return jsonObject;
        }).collect(Collectors.toList());

        PageVo answer = new PageVo(req.getInt("currentPage"), req.getInt("pageSize"));
        answer.setTotal(page.getTotal());
        answer.setData(collect);
        return Result.success(answer);
    }

    @Logs("前台列表")
    @Exclude(type = "list", value = {"id", "createBy", "createTime", "updateBy", "updateTime", "del", "status"})
    @GetMapping("/list")
    public Result<List<JSONObject>> adminList() {
        List<Banner> banners = bannerMapper.selectList(new LambdaQueryWrapper<Banner>().eq(Banner::getStatus, "是"));
        List<JSONObject> collect = banners.stream().map(item -> {
            JSONObject jsonObject = new JSONObject();
            BeanUtil.copyProperties(item, jsonObject);
            return jsonObject;
        }).collect(Collectors.toList());
        return Result.success(collect);
    }

    @Logs("激活")
    @GetMapping("/activate")
    public Result<Boolean> activate(@RequestParam("id") Long id, @RequestParam("status") int status) {
        Banner banner = new Banner();
        banner.setId(id);
        if (status == 1) {
            banner.setStatus("是");
        } else {
            banner.setStatus("否");
        }
        return Result.success(bannerMapper.updateById(banner) > 0);
    }
}
