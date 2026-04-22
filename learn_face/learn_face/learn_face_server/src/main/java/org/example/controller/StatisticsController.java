package org.example.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.annotation.Logs;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.mapper.CourseMapper;
import org.example.mapper.ExamMapper;
import org.example.mapper.GoalMapper;
import org.example.mapper.LearnLogMapper;
import org.example.pojo.base.Base;
import org.example.pojo.po.Course;
import org.example.pojo.po.Exam;
import org.example.pojo.po.Goal;
import org.example.pojo.po.LearnLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Resource
    private GoalMapper goalMapper;

    @Resource
    private LearnLogMapper learnLogMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private ExamMapper examMapper;

    @Logs("目标统计")
    @GetMapping("/goal")
    public Result<List<JSONObject>> goal() {
        // 1. 查询所有目标
        List<Goal> goals = goalMapper.selectList(new LambdaQueryWrapper<Goal>().eq(Base::getCreateBy, SecurityUtils.getUsername()));
        List<JSONObject> result = new ArrayList<>();
        for (Goal item : goals) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("title", item.getTitle());
            double progress = 0.0;
            if (item.getTargetValue() != 0) {
                progress = (double) item.getCurrentValue() / item.getTargetValue() * 100;
            }
            double finalProgress = Math.min(progress, 100.0);
            jsonObject.putOpt("progress", finalProgress);

            result.add(jsonObject);
        }
        return Result.success(result);
    }


    @Logs("学习总时长")
    @GetMapping("/metrics")
    public Result<List<JSONObject>> metrics() {

        List<JSONObject> answer = new ArrayList<>();

        String username = SecurityUtils.getUsername();
        //  总时长
        List<LearnLog> learnLogs = learnLogMapper.selectList(new LambdaQueryWrapper<LearnLog>().eq(Base::getCreateBy, username));
        long sum = learnLogs.stream().mapToLong(LearnLog::getDuration).sum();
        double learnedHours = Math.round(sum / 36.0) / 100.0;

        JSONObject totalDurationJsonObject = new JSONObject();
        totalDurationJsonObject.putOpt("label", "总学习时长");
        totalDurationJsonObject.putOpt("value", learnedHours);
        totalDurationJsonObject.putOpt("unit", "小时");
        totalDurationJsonObject.putOpt("icon", "Timer");
        totalDurationJsonObject.putOpt("colorClass", "icon-primary");
        answer.add(totalDurationJsonObject);

        //  总门数
        long totalRecords = learnLogs.stream().map(LearnLog::getLearnId).distinct().count();
        JSONObject totalRecordJsonObject = new JSONObject();
        totalRecordJsonObject.putOpt("label", "学习课程");
        totalRecordJsonObject.putOpt("value", totalRecords);
        totalRecordJsonObject.putOpt("unit", "门");
        totalRecordJsonObject.putOpt("icon", "Trophy");
        totalRecordJsonObject.putOpt("colorClass", "icon-warning");
        answer.add(totalRecordJsonObject);

        //  总做题量
        List<Exam> exams = examMapper.selectList(new LambdaQueryWrapper<Exam>().eq(Base::getCreateBy, username));
        int totalExam = 0;
        for (Exam exam : exams) {
            totalExam += exam.getAnswer().size();
        }

        JSONObject totalExamJsonObject = new JSONObject();
        totalExamJsonObject.putOpt("label", "刷题数量");
        totalExamJsonObject.putOpt("value", totalExam);
        totalExamJsonObject.putOpt("unit", "道");
        totalExamJsonObject.putOpt("icon", "DataAnalysis");
        totalExamJsonObject.putOpt("colorClass", "icon-success");
        answer.add(totalExamJsonObject);

        return Result.success(answer);
    }


    @Logs("学习时长统计")
    @GetMapping("/oneselfLearn")
    public Result<List<JSONObject>> oneselfLearn() {
        String username = SecurityUtils.getUsername();

        List<LearnLog> learnLogs = learnLogMapper.selectList(new LambdaQueryWrapper<LearnLog>().eq(Base::getCreateBy, username));
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Base::getCreateBy, username));

        Map<Long, Long> learnedTimeMap = learnLogs.stream().collect(Collectors.groupingBy(LearnLog::getLearnId, Collectors.summingLong(LearnLog::getDuration)));

        Map<Long, Course> courseMap = courses.stream().collect(Collectors.toMap(Course::getId, course -> course, (k1, k2) -> k1));

        // 5. 组装结果数据
        List<JSONObject> resultData = learnedTimeMap.entrySet().stream().map(entry -> {
            Long courseId = entry.getKey();
            Long totalSeconds = entry.getValue();
            Course course = courseMap.get(courseId);

            JSONObject json = new JSONObject();

            json.putOpt("courseName", course != null ? course.getName() : "未知课程");

            double learnedHours = totalSeconds / 3600.0;
            json.putOpt("learnedDuration", String.format("%.2f", learnedHours));

            double totalCourseHours = 0.0;
            if (course != null && StrUtil.isNotBlank(course.getDuration())) {
                totalCourseHours = parseDurationToHours(course.getDuration());
            }
            json.putOpt("courseTotalHours", totalCourseHours);

            if (totalCourseHours > 0) {
                double progress = (learnedHours / totalCourseHours) * 100;
                json.putOpt("progress", String.format("%.2f", progress) + "%");
            } else {
                json.putOpt("progress", "0%");
            }

            return json;
        }).collect(Collectors.toList());

        return Result.success(resultData);
    }

    /**
     * 辅助方法：解析课程时长字符串为小时数
     * 支持格式示例："1.5小时", "2小时", "90分钟", "120" (默认为小时)
     *
     * @param durationStr 时长字符串
     * @return 小时数
     */
    private double parseDurationToHours(String durationStr) {
        if (StrUtil.isBlank(durationStr)) {
            return 0.0;
        }

        String trimStr = durationStr.trim();

        try {
            // 1. 包含 "小时"
            if (trimStr.contains("小时")) {
                String numStr = trimStr.replace("小时", "").trim();
                return Double.parseDouble(numStr);
            }

            // 2. 包含 "分钟"
            if (trimStr.contains("分钟")) {
                String numStr = trimStr.replace("分钟", "").trim();
                double minutes = Double.parseDouble(numStr);
                return minutes / 60.0;
            }

            // 3. 纯数字，默认视为小时
            return Double.parseDouble(trimStr);

        } catch (NumberFormatException e) {
            // 解析失败返回0
            return 0.0;
        }
    }


    @Logs("学习时长统计")
    @GetMapping("/learn")
    public Result<List<JSONObject>> learn() {
        String username = SecurityUtils.getUsername();

        List<LearnLog> learnLogs = learnLogMapper.selectList(new LambdaQueryWrapper<LearnLog>().eq(Base::getCreateBy, username));
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>().eq(Base::getCreateBy, username));

        Map<Long, Long> learnedTimeMap = learnLogs.stream().collect(Collectors.groupingBy(LearnLog::getLearnId, Collectors.summingLong(LearnLog::getDuration)));

        Map<Long, Course> courseMap = courses.stream().collect(Collectors.toMap(Course::getId, course -> course, (k1, k2) -> k1));

        // 5. 组装结果数据
        List<JSONObject> resultData = learnedTimeMap.entrySet().stream().map(entry -> {
            Long courseId = entry.getKey();
            Long totalSeconds = entry.getValue();
            Course course = courseMap.get(courseId);

            JSONObject json = new JSONObject();

            json.putOpt("courseName", course != null ? course.getName() : "未知课程");

            // 新增归属人字段
            json.putOpt("createBy", username);

            double learnedHours = totalSeconds / 3600.0;
            json.putOpt("learnedDuration", String.format("%.2f", learnedHours));

            double totalCourseHours = 0.0;
            if (course != null && StrUtil.isNotBlank(course.getDuration())) {
                totalCourseHours = parseDurationToHours(course.getDuration());
            }
            json.putOpt("courseTotalHours", totalCourseHours);

            if (totalCourseHours > 0) {
                double progress = (learnedHours / totalCourseHours) * 100;
                json.putOpt("progress", String.format("%.2f", progress) + "%");
            } else {
                json.putOpt("progress", "0%");
            }

            return json;
        }).collect(Collectors.toList());

        return Result.success(resultData);
    }


    @Logs("目标统计")
    @GetMapping("/goalAnalyse")
    public Result<List<JSONObject>> goalAnalyse() {
        List<Goal> goals = goalMapper.selectList(null);

        // 统计总进度和已完成数量
        int totalGoals = goals.size();
        long completedCount = goals.stream().filter(goal -> "completed".equals(goal.getStatus())).count();
        long progressCount = goals.stream().filter(goal -> "progress".equals(goal.getStatus())).count();

        // 组装结果数据
        List<JSONObject> resultData = goals.stream().map(goal -> {
            JSONObject json = new JSONObject();
            json.putOpt("title", goal.getTitle());
            json.putOpt("status", goal.getStatus());

            // 计算该目标的完成情况（如果是已完成则为100%，否则根据实际逻辑计算，这里仅示例）
            if ("completed".equals(goal.getStatus())) {
                json.putOpt("completionRate", "100%");
            } else if ("progress".equals(goal.getStatus())) {
                // 如果有进度字段可以用 goal.getProgress()，否则默认为0或根据业务逻辑
                json.putOpt("completionRate", "0%");
            }
            return json;
        }).collect(Collectors.toList());

        // 如果需要返回汇总信息，也可以把 completedCount 和 progressCount 放进去
        JSONObject summary = new JSONObject();
        summary.putOpt("total", totalGoals);
        summary.putOpt("completedCount", completedCount);
        summary.putOpt("progressCount", progressCount);

        // 假设你想把汇总信息作为列表的一部分返回，或者单独返回
        // 这里直接返回列表，包含每个目标的完成情况
        return Result.success(resultData);
    }

    @Logs("分数统计")
    @GetMapping("/score")
    public Result<JSONObject> score() {
        List<Exam> exams = examMapper.selectList(null);

        // 分数分布统计
        Map<String, Long> distributionMap = exams.stream()
                .filter(e -> e.getFinScore() != null)
                .collect(Collectors.groupingBy(
                        e -> {
                            Integer score = e.getFinScore();
                            if (score >= 90) return "90分以上";
                            else if (score >= 80) return "80-89分";
                            else if (score >= 70) return "70-79分";
                            else if (score >= 60) return "60-69分";
                            else return "60分以下";
                        },
                        Collectors.counting()
                ));

        // 及格情况统计
        Map<String, Long> passMap = exams.stream()
                .filter(e -> e.getFinScore() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getFinScore() >= 60 ? "及格" : "不及格",
                        Collectors.counting()
                ));

        JSONObject result = new JSONObject();
        result.putOpt("distribution", distributionMap);
        result.putOpt("passStatus", passMap);

        return Result.success(result);
    }


}
