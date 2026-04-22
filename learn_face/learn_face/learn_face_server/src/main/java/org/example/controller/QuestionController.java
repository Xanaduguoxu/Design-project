package org.example.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.constant.Result;
import org.example.handler.LlmHandler;
import org.example.handler.PromptHandler;
import org.example.mapper.QuestionMapper;
import org.example.pojo.base.Base;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Question;
import org.example.utils.TagLabelUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/question")
public class QuestionController {

    @Resource
    private PromptHandler promptHandler;

    @Resource
    private LlmHandler llmHandler;

    @Resource
    private QuestionMapper questionMapper;

    @Logs("添加试题")
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        Question question = new Question();
        BeanUtil.copyProperties(req, question);
        normalizeTags(question);
        return Result.success(questionMapper.insert(question) > 0);
    }

    @Logs("删除试题")
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        return Result.success(questionMapper.deleteById(id) > 0);
    }

    @Logs("编辑试题")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Question question = new Question();
        BeanUtil.copyProperties(req, question);
        normalizeTags(question);
        return Result.success(questionMapper.updateById(question) > 0);
    }

    @Logs("试题列表")
    @Exclude(type = "page")
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<Question> page = new Page<>(req.getInt("currentPage"), req.getInt("pageSize"));
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        String keyword = req.getStr("keyword");
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> {
                wrapper.like(Question::getQuestion, keyword);
            });
        }
        queryWrapper.orderByDesc(Base::getCreateTime);
        questionMapper.selectPage(page, queryWrapper);
        fillAndPersistMissingTags(page.getRecords());
        PageVo answer = new PageVo(req.getInt("currentPage"), req.getInt("pageSize"));
        answer.setData(page.getRecords());
        answer.setTotal(page.getTotal());
        return Result.success(answer);
    }

    @Logs("ai出题")
    @Exclude(type = "details")
    @GetMapping("/ai")
    public Result<JSONObject> ai(@RequestParam("param") String param) throws Exception {
        String prompt = promptHandler.loadQuestionPrompt();
        List<Map<String, Object>> messages = new ArrayList<>(Collections.singletonList(llmHandler.assembleSystem(prompt)));
        messages.add(llmHandler.assembleUser(param));
        String resp = llmHandler.chat(messages).replace("```json", "").replace("```", "");
        Question bean = JSONUtil.toBean(resp, Question.class);
        normalizeTags(bean);
        return Result.success(new JSONObject(bean));
    }

    private void fillAndPersistMissingTags(List<Question> questions) {
        for (Question question : questions) {
            boolean changed = normalizeTags(question);
            if (changed && question.getId() != null) {
                Question updateBean = new Question();
                updateBean.setId(question.getId());
                updateBean.setType(question.getType());
                updateBean.setKnowledgePoint(question.getKnowledgePoint());
                questionMapper.updateById(updateBean);
            }
        }
    }

    private boolean normalizeTags(Question question) {
        if (question == null) {
            return false;
        }

        boolean changed = false;
        String normalizedType = TagLabelUtils.normalizeOrInferDifficultyTag(
                question.getType(),
                question.getScore(),
                question.getCategory(),
                question.getQuestion(),
                question.getParse()
        );
        if (!StrUtil.equals(question.getType(), normalizedType)) {
            question.setType(normalizedType);
            changed = true;
        }

        if (StrUtil.isBlank(question.getKnowledgePoint())) {
            question.setKnowledgePoint(TagLabelUtils.inferKnowledgePoint(
                    question.getQuestion(),
                    question.getParse(),
                    question.getAnswer(),
                    question.getCategory()
            ));
            changed = true;
        }

        return changed;
    }
}
