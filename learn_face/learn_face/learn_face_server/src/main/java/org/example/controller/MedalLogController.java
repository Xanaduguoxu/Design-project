package org.example.controller;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.annotation.Logs;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.mapper.MedalLogMapper;
import org.example.mapper.MedalMapper;
import org.example.pojo.base.Base;
import org.example.pojo.po.Medal;
import org.example.pojo.po.MedalLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/medalLog")
public class MedalLogController {

    @Resource
    private MedalMapper medalMapper;

    @Resource
    private MedalLogMapper medalLogMapper;

    @Logs("我的勋章")
    @GetMapping("/myself")
    public Result<List<JSONObject>> medalLog() {
        // 1. 查询当前用户已解锁的勋章日志
        List<MedalLog> unlockedLogs = medalLogMapper.selectList(
                new LambdaQueryWrapper<MedalLog>()
                        .eq(Base::getCreateBy, SecurityUtils.getUsername())
        );

        // 2. 查询系统中所有的勋章定义
        List<Medal> allMedals = medalMapper.selectList(null);

        // 3. 转换为 Map 以便根据 ID 快速查找已解锁记录，假设 MedalLog 中 medalId 对应 Medal 的主键 ID
        // 如果 Medal 实体没有 getId() 方法，请根据实际情况调整，例如使用 name 匹配
        Map<Long, MedalLog> unlockedMap = unlockedLogs.stream()
                .collect(Collectors.toMap(MedalLog::getMedalId, log -> log, (v1, v2) -> v1));

        List<JSONObject> result = new ArrayList<>();

        for (Medal medal : allMedals) {
            // 假设 Medal 实体有 getId() 方法，若无请改为 medal.getName() 并在下方做相应调整
            Long medalId = medal.getId();
            MedalLog log = unlockedMap.get(medalId);

            JSONObject json = new JSONObject();
            // 填充勋章基础信息
            json.put("name", medal.getName());
            json.put("brief", medal.getBrief());
            json.put("icon", medal.getIcon());

            if (log != null) {
                // 已解锁：显示日志中的详细信息
                json.put("unlocked", "1"); // 或者 true
                json.put("obtainedDate", log.getObtainedDate());
            } else {
                // 未解锁
                json.put("unlocked", "0"); // 或者 false
                json.put("obtainedDate", null);
            }

            result.add(json);
        }

        return Result.success(result);
    }

}
