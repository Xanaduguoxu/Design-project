package org.example.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.handler.EmailHandler;
import org.example.mapper.LearnLogMapper;
import org.example.pojo.po.LearnLog;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TaskSchedule {

    @Resource
    private LearnLogMapper learnLogMapper;

    @Resource
    private EmailHandler emailHandler;

    //  Cron表达式：每分钟的第0秒执行一次 (0 * * * * ?)
    @Scheduled(cron = "0 * * * * ?")
    public void notice() {
        LambdaQueryWrapper<LearnLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.apply("DATE(create_time) = CURDATE()");
        List<LearnLog> learnLogs = learnLogMapper.selectList(queryWrapper);

        //  按用户分组并计算总时长
        Map<String, Long> durationByUser = learnLogs.stream().collect(Collectors.groupingBy(LearnLog::getCreateBy, Collectors.summingLong(LearnLog::getDuration)));

        //  转换为小时并输出
        durationByUser.forEach((createBy, totalSeconds) -> {
            double totalHours = totalSeconds / 3600.0;
            if (totalHours > 5) {
                emailHandler.sendOfContext(createBy, "您已经学习5小时了,请休稍加休息");
            }
        });
    }
}
