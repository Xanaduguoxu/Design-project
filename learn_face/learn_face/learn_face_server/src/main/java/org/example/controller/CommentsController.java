package org.example.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.constant.Result;
import org.example.mapper.CommentsMapper;
import org.example.pojo.base.Base;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Comments;
import org.example.utils.ReqUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comments")
public class CommentsController {

    @Resource
    private CommentsMapper commentsMapper;

    @Logs("评论")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Comments comments = new Comments();
        BeanUtil.copyProperties(req, comments);
        return Result.success(commentsMapper.insert(comments) > 0);
    }

    @Logs("删除评论")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(commentsMapper.deleteById(id) > 0);
    }

    @Logs("评论")
    @Exclude(type = "page")
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<Comments> page = new Page<>(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));

        LambdaQueryWrapper<Comments> queryWrapper = new LambdaQueryWrapper<>();
        String keyword = req.getStr("keyword");
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper ->
                    wrapper.like(Comments::getReceiver, keyword)
                            .or()
                            .like(Comments::getReceiver, keyword)
            );
        }
        queryWrapper.orderByDesc(Base::getCreateTime);
        commentsMapper.selectPage(page, queryWrapper);
        PageVo answer = new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        answer.setTotal(page.getTotal());
        answer.setData(page.getRecords());
        return Result.success(answer);
    }

    @Logs("查看评论")
    @GetMapping("/allComments")
    public Result<JSONArray> allComments(@RequestParam("id") Long id) {


        List<Comments> allComments = commentsMapper.selectList(
                new LambdaQueryWrapper<Comments>()
                        .eq(Comments::getCorrelationId, id)
                        .orderByAsc(Comments::getCreateTime)
        );

        if (CollectionUtils.isEmpty(allComments)) {
            return Result.success(new JSONArray());
        }

        // 转换为JSONObject列表
        List<JSONObject> allList = allComments.stream()
                .map(this::toJSONObject)
                .collect(Collectors.toList());

        // 一级评论
        List<JSONObject> rootList = allList.stream()
                .filter(obj -> obj.getLong("parentId") == null)
                .collect(Collectors.toList());

        //  二级评论
        Map<Long, List<JSONObject>> childrenMap = allList.stream()
                .filter(obj -> obj.getLong("parentId") != null)
                .collect(Collectors.groupingBy(obj -> obj.getLong("parentId")));

        // 组装树形结构
        for (JSONObject root : rootList) {
            List<JSONObject> children = childrenMap.getOrDefault(root.getLong("id"), new ArrayList<>());
            root.putOpt("children", children);
            root.putOpt("childCount", children.size());
        }

        // 一级评论按时间倒序
        rootList.sort((a, b) -> {
            Date t1 = a.getDate("createTime");
            Date t2 = b.getDate("createTime");
            return t2.compareTo(t1);
        });


        return Result.success(new JSONArray(rootList));
    }

    private JSONObject toJSONObject(Comments comment) {
        JSONObject obj = new JSONObject();
        obj.putOpt("id", comment.getId());
        obj.putOpt("correlationId", comment.getCorrelationId());
        obj.putOpt("parentId", comment.getParentId());
        obj.putOpt("sender", comment.getSender());
        obj.putOpt("senderAvatar", comment.getSenderAvatar());
        obj.putOpt("receiver", comment.getReceiver());
        obj.putOpt("content", comment.getContent());
        obj.putOpt("createTime", comment.getCreateTime());
        obj.putOpt("images", comment.getImages());
        return obj;
    }

}
