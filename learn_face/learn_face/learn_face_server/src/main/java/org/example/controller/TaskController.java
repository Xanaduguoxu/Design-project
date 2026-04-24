package org.example.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.mapper.QuestionMapper;
import org.example.mapper.TaskMapper;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Question;
import org.example.pojo.po.Task;
import org.example.utils.ReqUtils;
import org.example.utils.TagLabelUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/task")
public class TaskController {

    private static final String CATEGORY_JUDGE = "\u5224\u65AD\u9898";
    private static final String CATEGORY_SINGLE = "\u5355\u9009\u9898";
    private static final String CATEGORY_MULTI = "\u591A\u9009\u9898";
    private static final String CATEGORY_ESSAY = "\u7B80\u7B54\u9898";
    private static final String PAPER_SOURCE_TEACHER = "teacher";
    private static final String PAPER_SOURCE_STUDENT = "student";
    private static final String GRADING_MODE_TEACHER = "teacher";
    private static final String GRADING_MODE_SELF = "self";

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Logs("Task Action")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        String createMode = StrUtil.blankToDefault(req.getStr("createMode"), "auto").toLowerCase(Locale.ROOT);
        if ("manual".equals(createMode)) {
            return addManualPaper(req);
        }
        return addAutoPaper(req);
    }

    private Result<Boolean> addAutoPaper(JSONObject req) {
        String paperName = req.getStr("name");
        if (StrUtil.isBlank(paperName)) {
            return Result.fail(false);
        }

        Integer targetScore = req.containsKey("totalScore") ? req.getInt("totalScore") : 100;
        if (targetScore == null || targetScore <= 0) {
            targetScore = 100;
        }
        String difficultyTag = getDifficultyTag(req);

        Map<String, Double> typeRatio = new HashMap<>();
        typeRatio.put(CATEGORY_JUDGE, 0.15);
        typeRatio.put(CATEGORY_SINGLE, 0.35);
        typeRatio.put(CATEGORY_MULTI, 0.30);
        typeRatio.put(CATEGORY_ESSAY, 0.20);

        Map<String, Integer> typeTargetScores = calculateTypeScores(targetScore, typeRatio);

        List<Question> selectedQuestions = new ArrayList<>();
        Map<String, List<Question>> selectedByType = new HashMap<>();

        for (Map.Entry<String, Integer> entry : typeTargetScores.entrySet()) {
            String type = entry.getKey();
            Integer typeTargetScore = entry.getValue();

            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Question::getCategory, type);
            queryWrapper.eq(StrUtil.isNotBlank(difficultyTag), Question::getType, difficultyTag);
            List<Question> typeQuestions = questionMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(typeQuestions)) {
                continue;
            }

            List<Question> typeSelected = knapsackSelect(typeQuestions, typeTargetScore);
            Integer currentScore = typeSelected.stream().mapToInt(Question::getScore).sum();
            if (!currentScore.equals(typeTargetScore)) {
                typeSelected = improvedGreedySelect(typeQuestions, typeTargetScore, 3);
            }

            selectedQuestions.addAll(typeSelected);
            selectedByType.put(type, typeSelected);
        }

        Integer totalScore = selectedQuestions.stream().mapToInt(Question::getScore).sum();
        if (!totalScore.equals(targetScore)) {
            boolean adjusted = globalAdjustQuestions(selectedQuestions, selectedByType, targetScore, typeRatio);
            if (!adjusted) {
                System.out.printf("Warning: failed to exactly match target score, target=%d, actual=%d%n", targetScore, totalScore);
            }
        }

        if (selectedQuestions.isEmpty()) {
            return Result.fail(false);
        }

        saveQuestionsAsPaper(
                paperName,
                targetScore,
                difficultyTag,
                selectedQuestions,
                PAPER_SOURCE_TEACHER,
                null,
                GRADING_MODE_TEACHER
        );
        return Result.success(true);
    }

    private Result<Boolean> addManualPaper(JSONObject req) {
        String paperName = req.getStr("name");
        if (StrUtil.isBlank(paperName)) {
            return Result.fail(false);
        }

        List<Long> questionIds = parseQuestionIds(req);
        if (questionIds.isEmpty()) {
            return Result.fail(false);
        }

        List<Question> selectedQuestions = questionMapper.selectBatchIds(questionIds);
        if (selectedQuestions.isEmpty()) {
            return Result.fail(false);
        }

        Map<Long, Question> questionMap = selectedQuestions.stream()
                .collect(Collectors.toMap(Question::getId, item -> item, (a, b) -> a));

        List<Question> orderedQuestions = new ArrayList<>();
        for (Long id : questionIds) {
            Question question = questionMap.get(id);
            if (question != null) {
                orderedQuestions.add(question);
            }
        }

        if (orderedQuestions.isEmpty()) {
            return Result.fail(false);
        }

        Integer totalScore = req.getInt("totalScore");
        if (totalScore == null || totalScore <= 0) {
            totalScore = orderedQuestions.stream().mapToInt(item -> item.getScore() == null ? 0 : item.getScore()).sum();
        }

        String difficultyTag = getDifficultyTag(req);
        saveQuestionsAsPaper(
                paperName,
                totalScore,
                difficultyTag,
                orderedQuestions,
                PAPER_SOURCE_TEACHER,
                null,
                GRADING_MODE_TEACHER
        );
        return Result.success(true);
    }

    private void saveQuestionsAsPaper(String paperName,
                                      Integer totalScore,
                                      String difficultyTag,
                                      List<Question> questions,
                                      String paperSource,
                                      Long ownerUserId,
                                      String gradingMode) {
        for (Question question : questions) {
            Task task = new Task();
            BeanUtil.copyProperties(question, task);
            task.setName(paperName);
            task.setTotalScore(totalScore);
            task.setChoice(serializeChoice(question.getChoice()));

            String normalizedType = StrUtil.isNotBlank(difficultyTag)
                    ? difficultyTag
                    : TagLabelUtils.normalizeOrInferDifficultyTag(
                    question.getType(),
                    question.getScore(),
                    question.getCategory(),
                    question.getQuestion(),
                    question.getParse()
            );
            task.setType(normalizedType);
            task.setKnowledgePoint(StrUtil.blankToDefault(
                    question.getKnowledgePoint(),
                    TagLabelUtils.inferKnowledgePoint(question.getQuestion(), question.getParse(), question.getAnswer(), question.getCategory())
            ));
            task.setPaperSource(normalizePaperSource(paperSource));
            task.setOwnerUserId(ownerUserId);
            task.setGradingMode(normalizeGradingMode(gradingMode));
            task.setId(null);
            taskMapper.insert(task);
        }
    }

    private String getDifficultyTag(JSONObject req) {
        String difficultyTag = req.getStr("difficultyTag");
        if (StrUtil.isBlank(difficultyTag)) {
            difficultyTag = req.getStr("type");
        }
        return StrUtil.blankToDefault(difficultyTag, "");
    }

    private List<Long> parseQuestionIds(JSONObject req) {
        JSONArray questionIdArray = req.getJSONArray("questionIds");
        List<Long> answer = new ArrayList<>();
        if (questionIdArray == null) {
            return answer;
        }
        for (Object item : questionIdArray) {
            Long id = parseLong(item);
            if (id != null) {
                answer.add(id);
            }
        }
        return answer;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private String normalizePaperSource(String source) {
        String value = StrUtil.blankToDefault(source, PAPER_SOURCE_TEACHER).trim().toLowerCase(Locale.ROOT);
        return PAPER_SOURCE_STUDENT.equals(value) ? PAPER_SOURCE_STUDENT : PAPER_SOURCE_TEACHER;
    }

    private String normalizeGradingMode(String mode) {
        String value = StrUtil.blankToDefault(mode, GRADING_MODE_TEACHER).trim().toLowerCase(Locale.ROOT);
        return GRADING_MODE_SELF.equals(value) ? GRADING_MODE_SELF : GRADING_MODE_TEACHER;
    }

    private void applyPaperSourceFilter(LambdaQueryWrapper<Task> wrapper, String paperSource) {
        if (PAPER_SOURCE_STUDENT.equals(paperSource)) {
            wrapper.eq(Task::getPaperSource, PAPER_SOURCE_STUDENT);
            return;
        }
        wrapper.and(item -> item.eq(Task::getPaperSource, PAPER_SOURCE_TEACHER).or().isNull(Task::getPaperSource));
    }

    private String buildPaperGroupKey(Task task) {
        if (task == null) {
            return "";
        }
        String name = StrUtil.blankToDefault(task.getName(), "");
        String source = normalizePaperSource(task.getPaperSource());
        String ownerId = task.getOwnerUserId() == null ? "0" : String.valueOf(task.getOwnerUserId());
        return name + "|" + source + "|" + ownerId;
    }

    private Long getCurrentUserIdSafe() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception ignore) {
            return null;
        }
    }

    private boolean isRootRoleSafe() {
        try {
            return "root".equals(SecurityUtils.getRole());
        } catch (Exception ignore) {
            return false;
        }
    }

    @Logs("Task Action")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(taskMapper.deleteById(id) > 0);
    }

    @Logs("Task Action")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Task task = new Task();
        BeanUtil.copyProperties(req, task);
        normalizeTaskTags(task, true);
        return Result.success(taskMapper.updateById(task) > 0);
    }

    @Logs("Task Action")
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

        List<Task> records = page.getRecords();
        fillAndPersistMissingTags(records);

        PageVo answer = new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        answer.setTotal(page.getTotal());
        answer.setData(records);
        return Result.success(answer);
    }

    @Logs("Task Action")
    @PostMapping("/paperList")
    public Result<PageVo> paperList(@RequestBody JSONObject req) {
        int currentPage = ReqUtils.getCurrentPage(req);
        int pageSize = ReqUtils.getPageSize(req);
        String keyword = ReqUtils.getKeyword(req);
        String paperSource = normalizePaperSource(req.getStr("paperSource"));
        Long ownerUserId = req.getLong("ownerUserId");
        boolean isRoot = isRootRoleSafe();
        Long currentUserId = getCurrentUserIdSafe();

        LambdaQueryWrapper<Task> queryWrapper = new LambdaQueryWrapper<>();
        applyPaperSourceFilter(queryWrapper, paperSource);
        if (PAPER_SOURCE_STUDENT.equals(paperSource)) {
            if (isRoot) {
                if (ownerUserId != null) {
                    queryWrapper.eq(Task::getOwnerUserId, ownerUserId);
                }
            } else {
                if (currentUserId == null) {
                    return Result.success(new PageVo(currentPage, pageSize, 0L, Collections.emptyList()));
                }
                queryWrapper.eq(Task::getOwnerUserId, currentUserId);
            }
        }
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.like(Task::getName, keyword);
        }
        queryWrapper.orderByDesc(Task::getCreateTime);

        List<Task> allTasks = taskMapper.selectList(queryWrapper);
        fillAndPersistMissingTags(allTasks);

        Map<String, List<Task>> grouped = allTasks.stream()
                .collect(Collectors.groupingBy(this::buildPaperGroupKey, LinkedHashMap::new, Collectors.toList()));

        List<JSONObject> papers = new ArrayList<>();
        for (Map.Entry<String, List<Task>> entry : grouped.entrySet()) {
            List<Task> paperQuestions = entry.getValue();
            if (paperQuestions.isEmpty()) {
                continue;
            }
            Task latest = paperQuestions.get(0);
            JSONObject paper = new JSONObject();
            paper.putOpt("name", latest.getName());
            paper.putOpt("type", latest.getType());
            paper.putOpt("difficultyTag", latest.getType());
            paper.putOpt("totalScore", latest.getTotalScore());
            paper.putOpt("questionCount", paperQuestions.size());
            paper.putOpt("createTime", latest.getCreateTime());
            paper.putOpt("createBy", latest.getCreateBy());
            paper.putOpt("paperSource", normalizePaperSource(latest.getPaperSource()));
            paper.putOpt("ownerUserId", latest.getOwnerUserId());
            paper.putOpt("gradingMode", normalizeGradingMode(latest.getGradingMode()));
            paper.putOpt("questionIds", paperQuestions.stream().map(Task::getId).collect(Collectors.toList()));
            papers.add(paper);
        }

        long total = papers.size();
        int fromIndex = Math.min(Math.max((currentPage - 1) * pageSize, 0), papers.size());
        int toIndex = Math.min(fromIndex + pageSize, papers.size());
        List<JSONObject> pageData = papers.subList(fromIndex, toIndex);

        return Result.success(new PageVo(currentPage, pageSize, total, pageData));
    }

    @Logs("Task Action")
    @GetMapping("/paperDetails")
    public Result<List<JSONObject>> paperDetails(@RequestParam("name") String name,
                                                 @RequestParam(value = "paperSource", required = false) String paperSource,
                                                 @RequestParam(value = "ownerUserId", required = false) Long ownerUserId) {
        String normalizedSource = normalizePaperSource(paperSource);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getName, name)
                .orderByAsc(Task::getCreateTime);
        applyPaperSourceFilter(wrapper, normalizedSource);
        if (PAPER_SOURCE_STUDENT.equals(normalizedSource)) {
            if (isRootRoleSafe()) {
                if (ownerUserId != null) {
                    wrapper.eq(Task::getOwnerUserId, ownerUserId);
                }
            } else {
                Long currentUserId = getCurrentUserIdSafe();
                if (currentUserId == null) {
                    return Result.success(Collections.emptyList());
                }
                wrapper.eq(Task::getOwnerUserId, currentUserId);
            }
        }

        List<Task> tasks = taskMapper.selectList(wrapper);
        fillAndPersistMissingTags(tasks);
        List<JSONObject> details = tasks.stream().map(JSONObject::new).collect(Collectors.toList());
        return Result.success(details);
    }

    @Logs("Task Action")
    @DeleteMapping("/delPaper")
    public Result<Boolean> delPaper(@RequestParam("name") String name,
                                    @RequestParam(value = "paperSource", required = false) String paperSource,
                                    @RequestParam(value = "ownerUserId", required = false) Long ownerUserId) {
        String normalizedSource = normalizePaperSource(paperSource);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>().eq(Task::getName, name);
        applyPaperSourceFilter(wrapper, normalizedSource);
        if (PAPER_SOURCE_STUDENT.equals(normalizedSource)) {
            if (isRootRoleSafe()) {
                if (ownerUserId != null) {
                    wrapper.eq(Task::getOwnerUserId, ownerUserId);
                }
            } else {
                Long currentUserId = getCurrentUserIdSafe();
                if (currentUserId == null) {
                    return Result.fail(false);
                }
                wrapper.eq(Task::getOwnerUserId, currentUserId);
            }
        }
        return Result.success(taskMapper.delete(wrapper) > 0);
    }

    @Logs("Task Action")
    @DeleteMapping("/delOwnPaper")
    public Result<Boolean> delOwnPaper(@RequestParam("name") String name) {
        Long userId = getCurrentUserIdSafe();
        if (userId == null || StrUtil.isBlank(name)) {
            return Result.fail(false);
        }
        int removed = taskMapper.delete(new LambdaQueryWrapper<Task>()
                .eq(Task::getName, name)
                .eq(Task::getPaperSource, PAPER_SOURCE_STUDENT)
                .eq(Task::getOwnerUserId, userId));
        return Result.success(removed > 0);
    }

    @GetMapping("/taskName")
    public Result<List<String>> taskName(@RequestParam(value = "paperSource", required = false) String paperSource) {
        String normalizedSource = normalizePaperSource(paperSource);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        applyPaperSourceFilter(wrapper, normalizedSource);
        if (PAPER_SOURCE_STUDENT.equals(normalizedSource) && !isRootRoleSafe()) {
            Long userId = getCurrentUserIdSafe();
            if (userId == null) {
                return Result.success(Collections.emptyList());
            }
            wrapper.eq(Task::getOwnerUserId, userId);
        }
        wrapper.orderByDesc(Task::getCreateTime);

        List<String> answer = taskMapper.selectList(wrapper).stream()
                .map(Task::getName)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        return Result.success(answer);
    }

    @Exclude(type = "list", value = {"createBy", "createTime", "updateBy", "updateTime", "del", "answer", "parse"})
    @GetMapping("/selectTask")
    public Result<List<JSONObject>> selectTask(@RequestParam("name") String name,
                                               @RequestParam(value = "paperSource", required = false) String paperSource,
                                               @RequestParam(value = "ownerUserId", required = false) Long ownerUserId) {
        String normalizedSource = normalizePaperSource(paperSource);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>().eq(Task::getName, name);
        applyPaperSourceFilter(wrapper, normalizedSource);
        if (PAPER_SOURCE_STUDENT.equals(normalizedSource)) {
            if (isRootRoleSafe()) {
                if (ownerUserId != null) {
                    wrapper.eq(Task::getOwnerUserId, ownerUserId);
                }
            } else {
                Long userId = getCurrentUserIdSafe();
                if (userId == null) {
                    return Result.success(Collections.emptyList());
                }
                wrapper.eq(Task::getOwnerUserId, userId);
            }
        }

        List<Task> tasks = taskMapper.selectList(wrapper);
        fillAndPersistMissingTags(tasks);
        List<JSONObject> answer = tasks.stream().map(JSONObject::new).collect(Collectors.toList());
        return Result.success(answer);
    }

    @Logs("Task Action")
    @Exclude(type = "list", value = {"createBy", "createTime", "updateBy", "updateTime", "del", "answer", "parse"})
    @GetMapping("/ranTask")
    public Result<List<JSONObject>> ranTask(@RequestParam(value = "paperSource", required = false) String paperSource) {
        String normalizedSource = normalizePaperSource(paperSource);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        applyPaperSourceFilter(wrapper, normalizedSource);
        if (PAPER_SOURCE_STUDENT.equals(normalizedSource) && !isRootRoleSafe()) {
            Long userId = getCurrentUserIdSafe();
            if (userId == null) {
                return Result.success(Collections.emptyList());
            }
            wrapper.eq(Task::getOwnerUserId, userId);
        }

        List<Task> tasks = taskMapper.selectList(wrapper);
        if (tasks.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        fillAndPersistMissingTags(tasks);
        List<String> collect = tasks.stream()
                .map(Task::getName)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (collect.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        Collections.shuffle(collect);
        String name = collect.get(0);
        List<JSONObject> answer = tasks.stream().filter(item -> item.getName().equals(name)).map(JSONObject::new).collect(Collectors.toList());
        return Result.success(answer);
    }

    private void fillAndPersistMissingTags(List<Task> tasks) {
        for (Task task : tasks) {
            boolean changed = normalizeTaskTags(task, false);
            if (changed && task.getId() != null) {
                Task updateBean = new Task();
                updateBean.setId(task.getId());
                updateBean.setChoice(task.getChoice());
                updateBean.setType(task.getType());
                updateBean.setKnowledgePoint(task.getKnowledgePoint());
                taskMapper.updateById(updateBean);
            }
        }
    }

    private boolean normalizeTaskTags(Task task, boolean forceInferenceWhenInvalidType) {
        if (task == null) {
            return false;
        }

        boolean changed = false;
        String normalizedChoice = normalizeChoiceText(task.getChoice());
        if (!StrUtil.equals(task.getChoice(), normalizedChoice)) {
            task.setChoice(normalizedChoice);
            changed = true;
        }

        String normalizedType = TagLabelUtils.normalizeOrInferDifficultyTag(
                task.getType(),
                task.getScore(),
                task.getCategory(),
                task.getQuestion(),
                task.getParse()
        );
        boolean shouldReplaceType = forceInferenceWhenInvalidType || !TagLabelUtils.isValidDifficultyTag(task.getType());
        if (shouldReplaceType && !StrUtil.equals(task.getType(), normalizedType)) {
            task.setType(normalizedType);
            changed = true;
        }

        if (StrUtil.isBlank(task.getKnowledgePoint())) {
            task.setKnowledgePoint(TagLabelUtils.inferKnowledgePoint(
                    task.getQuestion(),
                    task.getParse(),
                    task.getAnswer(),
                    task.getCategory()
            ));
            changed = true;
        }

        return changed;
    }

    private String serializeChoice(List<String> choices) {
        if (choices == null) {
            return null;
        }
        return JSONUtil.toJsonStr(choices);
    }

    private String normalizeChoiceText(String rawChoice) {
        if (StrUtil.isBlank(rawChoice)) {
            return rawChoice;
        }

        String text = rawChoice.trim();
        if (isJsonArray(text)) {
            return text;
        }

        List<String> parsed = parseLegacyChoice(text);
        if (parsed.isEmpty()) {
            return rawChoice;
        }
        return JSONUtil.toJsonStr(parsed);
    }

    private boolean isJsonArray(String text) {
        try {
            return JSONUtil.parse(text) instanceof JSONArray;
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> parseLegacyChoice(String text) {
        String cleaned = text;
        if (cleaned.startsWith("[") && cleaned.endsWith("]") && cleaned.length() >= 2) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        String normalized = cleaned
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        if (normalized.contains("\n")) {
            String[] lines = normalized.split("\\r?\\n");
            for (String line : lines) {
                String item = line.trim();
                if (StrUtil.isNotBlank(item)) {
                    result.add(item);
                }
            }
            return result;
        }

        Matcher matcher = Pattern.compile("(?i)[A-H][\\.\\)]\\s*").matcher(normalized);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        if (starts.size() > 1) {
            for (int i = 0; i < starts.size(); i++) {
                int start = starts.get(i);
                int end = (i + 1 < starts.size()) ? starts.get(i + 1) : normalized.length();
                String item = normalized.substring(start, end).trim();
                if (StrUtil.isNotBlank(item)) {
                    result.add(item);
                }
            }
            return result;
        }

        String[] parts = normalized.split("\\s*[,;]\\s*");
        if (parts.length <= 1 && normalized.contains("  ")) {
            parts = normalized.split("\\s{2,}");
        }
        for (String part : parts) {
            String item = part.trim();
            if (StrUtil.isNotBlank(item)) {
                result.add(item);
            }
        }
        return result;
    }

    private Map<String, Integer> calculateTypeScores(Integer targetScore, Map<String, Double> ratios) {
        Map<String, Integer> typeScores = new HashMap<>();
        int totalAssigned = 0;

        for (Map.Entry<String, Double> entry : ratios.entrySet()) {
            int score = (int) Math.floor(targetScore * entry.getValue());
            typeScores.put(entry.getKey(), score);
            totalAssigned += score;
        }

        int remaining = targetScore - totalAssigned;
        if (remaining > 0) {
            List<Map.Entry<String, Double>> sortedRatios = ratios.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .collect(Collectors.toList());

            for (Map.Entry<String, Double> entry : sortedRatios) {
                if (remaining <= 0) {
                    break;
                }
                String type = entry.getKey();
                typeScores.put(type, typeScores.get(type) + 1);
                remaining--;
            }
        }

        return typeScores;
    }

    private List<Question> knapsackSelect(List<Question> questions, int targetScore) {
        if (questions == null || questions.isEmpty() || targetScore <= 0) {
            return new ArrayList<>();
        }

        @SuppressWarnings("unchecked")
        List<Question>[] dp = new ArrayList[targetScore + 1];
        dp[0] = new ArrayList<>();

        questions.sort((q1, q2) -> q2.getScore().compareTo(q1.getScore()));

        for (Question question : questions) {
            int score = question.getScore();

            for (int i = targetScore; i >= score; i--) {
                if (dp[i - score] != null && dp[i] == null) {
                    dp[i] = new ArrayList<>(dp[i - score]);
                    dp[i].add(question);
                    if (i == targetScore) {
                        return dp[targetScore];
                    }
                }
            }
        }

        for (int i = targetScore; i >= 0; i--) {
            if (dp[i] != null) {
                return dp[i];
            }
        }

        return new ArrayList<>();
    }

    private List<Question> improvedGreedySelect(List<Question> questions, Integer targetScore, int maxAttempts) {
        if (questions == null || questions.isEmpty()) {
            return new ArrayList<>();
        }

        List<Question> bestResult = new ArrayList<>();
        int bestDiff = Integer.MAX_VALUE;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            List<Question> shuffled = new ArrayList<>(questions);
            Collections.shuffle(shuffled);

            List<Question> currentResult = new ArrayList<>();
            int currentScore = 0;

            for (Question q : shuffled) {
                if (currentScore + q.getScore() <= targetScore) {
                    currentResult.add(q);
                    currentScore += q.getScore();
                    if (currentScore == targetScore) {
                        break;
                    }
                }
            }

            int currentDiff = Math.abs(targetScore - currentScore);
            if (currentDiff == 0) {
                return currentResult;
            }

            if (currentDiff < bestDiff) {
                bestDiff = currentDiff;
                bestResult = new ArrayList<>(currentResult);
            }
        }

        return bestResult;
    }

    private boolean globalAdjustQuestions(List<Question> allQuestions,
                                          Map<String, List<Question>> questionsByType,
                                          Integer targetScore,
                                          Map<String, Double> ratios) {
        int currentScore = allQuestions.stream().mapToInt(Question::getScore).sum();
        int diff = targetScore - currentScore;

        if (diff == 0) {
            return true;
        }

        List<Question> allAvailableQuestions = questionMapper.selectList(null);
        Map<Integer, List<Question>> questionsByScore = allAvailableQuestions.stream()
                .collect(Collectors.groupingBy(Question::getScore));

        Set<Long> selectedIds = allQuestions.stream().map(Question::getId).collect(Collectors.toSet());

        if (diff > 0) {
            return addQuestionsGlobally(allQuestions, diff, questionsByScore, selectedIds, ratios);
        } else {
            return removeQuestionsGlobally(allQuestions, -diff, questionsByType, ratios);
        }
    }

    private boolean addQuestionsGlobally(List<Question> allQuestions, int diff,
                                         Map<Integer, List<Question>> questionsByScore,
                                         Set<Long> selectedIds,
                                         Map<String, Double> ratios) {
        if (questionsByScore.containsKey(diff)) {
            Question candidate = findAvailableQuestion(questionsByScore.get(diff), selectedIds);
            if (candidate != null) {
                allQuestions.add(candidate);
                return true;
            }
        }

        List<String> orderedTypes = ratios.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String type : orderedTypes) {
            List<Question> typeQuestions = questionMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getCategory, type)
                            .notIn(!selectedIds.isEmpty(), Question::getId, selectedIds)
            );

            if (typeQuestions.isEmpty()) {
                continue;
            }

            for (Question candidate : typeQuestions) {
                if (candidate.getScore() <= diff) {
                    allQuestions.add(candidate);
                    selectedIds.add(candidate.getId());
                    diff -= candidate.getScore();

                    if (diff == 0) {
                        return true;
                    }
                }
            }
        }

        return diff == 0;
    }

    private boolean removeQuestionsGlobally(List<Question> allQuestions, int diff,
                                            Map<String, List<Question>> questionsByType,
                                            Map<String, Double> ratios) {
        List<String> orderedTypes = ratios.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e1.getValue(), e2.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Iterator<Question> iterator = allQuestions.iterator();
        while (iterator.hasNext()) {
            Question question = iterator.next();
            if (question.getScore() == diff) {
                iterator.remove();
                return true;
            }
        }

        for (String type : orderedTypes) {
            List<Question> typeQuestions = questionsByType.get(type);
            if (typeQuestions == null || typeQuestions.isEmpty()) {
                continue;
            }

            typeQuestions.sort((q1, q2) -> q2.getScore().compareTo(q1.getScore()));

            Iterator<Question> typeIterator = typeQuestions.iterator();
            while (typeIterator.hasNext() && diff > 0) {
                Question question = typeIterator.next();
                if (question.getScore() <= diff) {
                    typeIterator.remove();
                    allQuestions.remove(question);
                    diff -= question.getScore();

                    if (diff == 0) {
                        return true;
                    }
                }
            }
        }

        return diff == 0;
    }

    private Question findAvailableQuestion(List<Question> candidates, Set<Long> selectedIds) {
        for (Question q : candidates) {
            if (!selectedIds.contains(q.getId())) {
                return q;
            }
        }
        return null;
    }
}
