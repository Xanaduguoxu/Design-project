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

    @Logs("评论分页")
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
    public Result<JSONArray> allComments(@RequestParam("id") Long id,
                                         @RequestParam(value = "belong", required = false) String belong) {
        LambdaQueryWrapper<Comments> queryWrapper = new LambdaQueryWrapper<Comments>()
                .eq(Comments::getCorrelationId, id);
        if (StrUtil.isNotBlank(belong)) {
            queryWrapper.eq(Comments::getBelong, belong);
        }
        queryWrapper.orderByAsc(Comments::getCreateTime);

        List<Comments> allComments = commentsMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(allComments)) {
            return Result.success(new JSONArray());
        }

        Map<Long, List<Comments>> childrenMap = allComments.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(Comments::getParentId));

        List<Comments> rootList = allComments.stream()
                .filter(item -> item.getParentId() == null)
                .sorted((a, b) -> compareTimeDesc(a.getCreateTime(), b.getCreateTime()))
                .collect(Collectors.toList());

        JSONArray result = new JSONArray();
        for (Comments root : rootList) {
            result.add(buildCommentTree(root, childrenMap));
        }
        return Result.success(result);
    }

    private JSONObject buildCommentTree(Comments node, Map<Long, List<Comments>> childrenMap) {
        JSONObject jsonNode = toJSONObject(node);
        List<Comments> children = childrenMap.getOrDefault(node.getId(), new ArrayList<>());
        children.sort((a, b) -> compareTimeAsc(a.getCreateTime(), b.getCreateTime()));

        JSONArray childrenArr = new JSONArray();
        for (Comments child : children) {
            childrenArr.add(buildCommentTree(child, childrenMap));
        }

        jsonNode.putOpt("children", childrenArr);
        jsonNode.putOpt("childCount", children.size());
        return jsonNode;
    }

    private int compareTimeAsc(Date t1, Date t2) {
        if (t1 == null && t2 == null) {
            return 0;
        }
        if (t1 == null) {
            return -1;
        }
        if (t2 == null) {
            return 1;
        }
        return t1.compareTo(t2);
    }

    private int compareTimeDesc(Date t1, Date t2) {
        if (t1 == null && t2 == null) {
            return 0;
        }
        if (t1 == null) {
            return 1;
        }
        if (t2 == null) {
            return -1;
        }
        return t2.compareTo(t1);
    }

    private JSONObject toJSONObject(Comments comment) {
        JSONObject obj = new JSONObject();
        obj.putOpt("id", comment.getId() == null ? null : comment.getId().toString());
        obj.putOpt("correlationId", comment.getCorrelationId() == null ? null : comment.getCorrelationId().toString());
        obj.putOpt("parentId", comment.getParentId() == null ? null : comment.getParentId().toString());
        obj.putOpt("sender", comment.getSender());
        obj.putOpt("senderAvatar", comment.getSenderAvatar());
        obj.putOpt("receiver", comment.getReceiver());
        obj.putOpt("content", comment.getContent());
        obj.putOpt("createTime", comment.getCreateTime());
        obj.putOpt("images", comment.getImages());
        obj.putOpt("belong", comment.getBelong());
        return obj;
    }
}
