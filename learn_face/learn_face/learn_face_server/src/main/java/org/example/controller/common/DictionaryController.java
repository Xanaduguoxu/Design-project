package org.example.controller.common;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.constant.Result;
import org.example.mapper.common.DictionaryMapper;
import org.example.pojo.base.Dictionary;
import org.example.pojo.base.PageVo;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dictionary")
public class DictionaryController {


    @Resource
    private DictionaryMapper dictionaryMapper;

    @Logs("添加字典")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Dictionary dictionary = new Dictionary();
        BeanUtil.copyProperties(req, dictionary);
        return Result.success(dictionaryMapper.insert(dictionary) > 0);
    }

    @Logs("删除字典")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(dictionaryMapper.deleteById(id) > 0);
    }

    @Logs("更新字典")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Dictionary dictionary = new Dictionary();
        BeanUtil.copyProperties(req, dictionary);
        return Result.success(dictionaryMapper.updateById(dictionary) > 0);
    }

    @Logs("字典后台")
    @PostMapping("/admin/list")
    public Result<PageVo> admins(@RequestBody JSONObject req) {
        Page<Dictionary> page = new Page<>(req.getInt("currentPage"), req.getInt("pageSize"));
        LambdaQueryWrapper<Dictionary> queryWrapper = new LambdaQueryWrapper<>();
        String keyword = req.getStr("keyword");
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper ->
                    wrapper.like(Dictionary::getCode, keyword)
                            .or()
                            .like(Dictionary::getValue, keyword)
                            .or()
                            .like(Dictionary::getStatus, keyword)
            );
        }
        dictionaryMapper.selectPage(page, queryWrapper);
        PageVo answer = new PageVo(req.getInt("currentPage"), req.getInt("pageSize"));
        answer.setTotal(page.getTotal());
        answer.setData(page.getRecords());
        return Result.success(answer);
    }

    @Logs("字典前台")
    @Exclude(type = "list", value = {"id", "createBy", "createTime", "updateBy", "updateTime", "del", "status"})
    @GetMapping("/list")
    public Result<List<JSONObject>> list(@RequestParam("classify") String classify) {
        List<Dictionary> dictionaries = dictionaryMapper.selectList(new LambdaQueryWrapper<>(Dictionary.class).eq(Dictionary::getStatus, "是").eq(Dictionary::getClassify, classify));
        List<JSONObject> answer = dictionaries.stream().map(item -> {
            JSONObject jsonObject = new JSONObject();
            BeanUtil.copyProperties(item, jsonObject);
            return jsonObject;
        }).collect(Collectors.toList());
        return Result.success(answer);
    }

}
