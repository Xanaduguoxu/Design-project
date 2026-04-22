package org.example.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.constant.Result;
import org.example.mapper.QuestionMapper;
import org.example.mapper.TaskMapper;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Question;
import org.example.pojo.po.Task;
import org.example.utils.ReqUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/task")
public class TaskController {

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private QuestionMapper questionMapper;


    @Logs("添加任务")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Integer targetScore = req.containsKey("totalScore") ? req.getInt("totalScore") : 100;

        // 默认题型比例
        Map<String, Double> typeRatio = new HashMap<>();
        typeRatio.put("判断题", 0.15);
        typeRatio.put("单选题", 0.35);
        typeRatio.put("多选题", 0.30);
        typeRatio.put("简答题", 0.20);

        // 计算各题型目标分数（确保总和等于目标分数）
        Map<String, Integer> typeTargetScores = calculateTypeScores(targetScore, typeRatio);

        List<Question> selectedQuestions = new ArrayList<>();
        Map<String, List<Question>> selectedByType = new HashMap<>();

        // 为每种题型选择题目
        for (Map.Entry<String, Integer> entry : typeTargetScores.entrySet()) {
            String type = entry.getKey();
            Integer typeTargetScore = entry.getValue();

            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Question::getCategory, type);
            queryWrapper.eq(StrUtil.isNotBlank(req.getStr("type")), Question::getType, req.get("type"));
            // 获取该题型所有题目
            List<Question> typeQuestions = questionMapper.selectList(queryWrapper);

            if (CollectionUtils.isEmpty(typeQuestions)) {
                continue;
            }

            // 使用动态规划选择题目组合
            List<Question> typeSelected = knapsackSelect(typeQuestions, typeTargetScore);

            // 如果动态规划无法精确匹配，使用改进的贪心算法
            Integer currentScore = typeSelected.stream().mapToInt(Question::getScore).sum();
            if (!currentScore.equals(typeTargetScore)) {
                typeSelected = improvedGreedySelect(typeQuestions, typeTargetScore, 3);
            }

            selectedQuestions.addAll(typeSelected);
            selectedByType.put(type, typeSelected);
        }

        // 验证并调整总分
        Integer totalScore = selectedQuestions.stream().mapToInt(Question::getScore).sum();
        if (!totalScore.equals(targetScore)) {
            boolean adjusted = globalAdjustQuestions(selectedQuestions, selectedByType, targetScore, typeRatio);
            if (!adjusted) {
                // 记录警告日志
                System.out.printf("警告: 无法精确匹配目标分数，目标: %d, 实际: %d%n",
                        targetScore, totalScore);
            }
        }

        // 这里可以添加保存试卷的逻辑
        System.out.printf("组卷完成: 目标分数=%d, 实际分数=%d, 题目数量=%d%n",
                targetScore,
                selectedQuestions.stream().mapToInt(Question::getScore).sum(),
                selectedQuestions.size());
        for (Question question : selectedQuestions) {
            Task task = new Task();
            BeanUtil.copyProperties(question, task);
            task.setName(req.getStr("name"));
            task.setTotalScore(req.getInt("totalScore"));
            task.setId(null);
            taskMapper.insert(task);
        }
        return Result.success(true);
    }

    @Logs("删除任务")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(taskMapper.deleteById(id) > 0);
    }

    @Logs("更新任务")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Task task = new Task();
        BeanUtil.copyProperties(req, task);
        return Result.success(taskMapper.updateById(task) > 0);
    }

    @Logs("查询任务")
    @Exclude(type = "page")
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<Task> page = new Page<>(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        String keyword = ReqUtils.getKeyword(req);
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(Task::getName, keyword));
        }
        queryWrapper.orderByDesc(Task::getCreateTime);
        taskMapper.selectPage(page, queryWrapper);
        PageVo answer = new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        answer.setTotal(page.getTotal());
        answer.setData(page.getRecords());
        return Result.success(answer);
    }

    @GetMapping("/taskName")
    public Result<List<String>> taskName() {
        List<String> answer = taskMapper.selectList(null).stream().map(Task::getName).distinct().collect(Collectors.toList());
        return Result.success(answer);
    }

    @Exclude(type = "list", value = {"createBy", "createTime", "updateBy", "updateTime", "del", "answer", "parse"})
    @GetMapping("/selectTask")
    public Result<List<JSONObject>> ranTask(@RequestParam("name") String name) {
        List<JSONObject> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>().eq(Task::getName, name)).stream().map(JSONObject::new).collect(Collectors.toList());
        return Result.success(tasks);
    }

    @Logs("随机试卷")
    @Exclude(type = "list", value = {"createBy", "createTime", "updateBy", "updateTime", "del", "answer", "parse"})
    @GetMapping("/ranTask")
    public Result<List<JSONObject>> ranTask() {
        List<Task> tasks = taskMapper.selectList(null);
        List<String> collect = tasks.stream().map(Task::getName).collect(Collectors.toList());
        Collections.shuffle(collect);
        String name = collect.get(0);
        List<JSONObject> collect1 = tasks.stream().filter(item -> item.getName().equals(name)).map(JSONObject::new).collect(Collectors.toList());
        return Result.success(collect1);
    }


    /**
     * 计算各题型目标分数（确保总和等于目标分数）
     */
    private Map<String, Integer> calculateTypeScores(Integer targetScore, Map<String, Double> ratios) {
        Map<String, Integer> typeScores = new HashMap<>();
        int totalAssigned = 0;

        // 第一轮：向下取整分配
        for (Map.Entry<String, Double> entry : ratios.entrySet()) {
            int score = (int) Math.floor(targetScore * entry.getValue());
            typeScores.put(entry.getKey(), score);
            totalAssigned += score;
        }

        // 第二轮：分配剩余分数（按比例从大到小）
        int remaining = targetScore - totalAssigned;
        if (remaining > 0) {
            List<Map.Entry<String, Double>> sortedRatios = ratios.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .collect(Collectors.toList());

            for (Map.Entry<String, Double> entry : sortedRatios) {
                if (remaining <= 0) break;

                String type = entry.getKey();
                typeScores.put(type, typeScores.get(type) + 1);
                remaining--;
            }
        }

        return typeScores;
    }

    /**
     * 动态规划选择题目组合（0-1背包问题变种）
     */
    private List<Question> knapsackSelect(List<Question> questions, int targetScore) {
        if (questions == null || questions.isEmpty() || targetScore <= 0) {
            return new ArrayList<>();
        }

        // dp[i]表示分数i是否可达，以及对应的题目列表
        @SuppressWarnings("unchecked")
        List<Question>[] dp = new ArrayList[targetScore + 1];
        dp[0] = new ArrayList<>();

        // 按分数排序，提高效率
        questions.sort((q1, q2) -> q2.getScore().compareTo(q1.getScore()));

        for (Question question : questions) {
            int score = question.getScore();

            for (int i = targetScore; i >= score; i--) {
                if (dp[i - score] != null && dp[i] == null) {
                    dp[i] = new ArrayList<>(dp[i - score]);
                    dp[i].add(question);

                    // 如果找到精确匹配，立即返回
                    if (i == targetScore) {
                        return dp[targetScore];
                    }
                }
            }
        }

        // 返回最接近目标分数的组合
        for (int i = targetScore; i >= 0; i--) {
            if (dp[i] != null) {
                return dp[i];
            }
        }

        return new ArrayList<>();
    }

    /**
     * 改进的贪心算法选择题目组合
     */
    private List<Question> improvedGreedySelect(List<Question> questions, Integer targetScore, int maxAttempts) {
        if (questions == null || questions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Question> bestResult = new ArrayList<>();
        int bestDiff = Integer.MAX_VALUE;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // 每次尝试前打乱题目顺序
            List<Question> shuffled = new ArrayList<>(questions);
            Collections.shuffle(shuffled);

            List<Question> currentResult = new ArrayList<>();
            int currentScore = 0;

            // 贪心选择
            for (Question q : shuffled) {
                if (currentScore + q.getScore() <= targetScore) {
                    currentResult.add(q);
                    currentScore += q.getScore();
                    if (currentScore == targetScore) {
                        break;
                    }
                }
            }

            // 评估结果
            int currentDiff = Math.abs(targetScore - currentScore);
            if (currentDiff == 0) {
                return currentResult; // 找到完美匹配
            }

            if (currentDiff < bestDiff) {
                bestDiff = currentDiff;
                bestResult = new ArrayList<>(currentResult);
            }
        }

        return bestResult;
    }

    /**
     * 全局调整题目组合
     */
    private boolean globalAdjustQuestions(List<Question> allQuestions,
                                          Map<String, List<Question>> questionsByType,
                                          Integer targetScore,
                                          Map<String, Double> ratios) {
        int currentScore = allQuestions.stream().mapToInt(Question::getScore).sum();
        int diff = targetScore - currentScore;

        if (diff == 0) {
            return true;
        }

        // 获取所有可用题目
        List<Question> allAvailableQuestions = questionMapper.selectList(null);
        Map<Integer, List<Question>> questionsByScore = allAvailableQuestions.stream()
                .collect(Collectors.groupingBy(Question::getScore));

        Set<Long> selectedIds = allQuestions.stream().map(Question::getId).collect(Collectors.toSet());

        if (diff > 0) {
            // 需要添加题目
            return addQuestionsGlobally(allQuestions, diff, questionsByScore, selectedIds, ratios);
        } else {
            // 需要移除题目
            return removeQuestionsGlobally(allQuestions, -diff, questionsByType, ratios);
        }
    }

    /**
     * 全局添加题目
     */
    private boolean addQuestionsGlobally(List<Question> allQuestions, int diff,
                                         Map<Integer, List<Question>> questionsByScore,
                                         Set<Long> selectedIds,
                                         Map<String, Double> ratios) {
        // 优先寻找刚好匹配的题目
        if (questionsByScore.containsKey(diff)) {
            Question candidate = findAvailableQuestion(questionsByScore.get(diff), selectedIds);
            if (candidate != null) {
                allQuestions.add(candidate);
                return true;
            }
        }

        // 按题型比例优先添加
        List<String> orderedTypes = ratios.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 尝试为各题型添加题目
        for (String type : orderedTypes) {
            List<Question> typeQuestions = questionMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getCategory, type)
                            .notIn(!selectedIds.isEmpty(), Question::getId, selectedIds)
            );

            if (typeQuestions.isEmpty()) continue;

            // 寻找适合的题目
            for (Question candidate : typeQuestions) {
                if (candidate.getScore() <= diff) {
                    allQuestions.add(candidate);
                    selectedIds.add(candidate.getId());
                    diff -= candidate.getScore();

                    if (diff == 0) return true;
                }
            }
        }

        return diff == 0;
    }

    /**
     * 全局移除题目
     */
    private boolean removeQuestionsGlobally(List<Question> allQuestions, int diff,
                                            Map<String, List<Question>> questionsByType,
                                            Map<String, Double> ratios) {
        // 按题型比例逆序尝试移除（先移除比例小的题型）
        List<String> orderedTypes = ratios.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e1.getValue(), e2.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 尝试移除刚好等于差值的题目
        Iterator<Question> iterator = allQuestions.iterator();
        while (iterator.hasNext()) {
            Question question = iterator.next();
            if (question.getScore() == diff) {
                iterator.remove();
                return true;
            }
        }

        // 按题型尝试移除
        for (String type : orderedTypes) {
            List<Question> typeQuestions = questionsByType.get(type);
            if (typeQuestions == null || typeQuestions.isEmpty()) continue;

            // 按分数从大到小排序
            typeQuestions.sort((q1, q2) -> q2.getScore().compareTo(q1.getScore()));

            Iterator<Question> typeIterator = typeQuestions.iterator();
            while (typeIterator.hasNext() && diff > 0) {
                Question question = typeIterator.next();
                if (question.getScore() <= diff) {
                    typeIterator.remove();
                    allQuestions.remove(question);
                    diff -= question.getScore();

                    if (diff == 0) return true;
                }
            }
        }

        return diff == 0;
    }

    /**
     * 从候选题目中找一个未被选中的题目
     */
    private Question findAvailableQuestion(List<Question> candidates, Set<Long> selectedIds) {
        for (Question q : candidates) {
            if (!selectedIds.contains(q.getId())) {
                return q;
            }
        }
        return null;
    }
}