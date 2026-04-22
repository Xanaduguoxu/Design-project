package org.example.handler;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.mapper.algorithm.BehaviorMapper;
import org.example.mapper.algorithm.VectorsMapper;
import org.example.mapper.algorithm.WeightMapper;
import org.example.pojo.po.algorithm.Recommend;
import org.example.pojo.base.Base;
import org.example.pojo.po.algorithm.Action;
import org.example.pojo.po.algorithm.Vectors;
import org.example.pojo.po.algorithm.Weight;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AlgorithmHandler {

    @Resource
    private BehaviorMapper behaviorMapper;

    @Resource
    private VectorsMapper vectorsMapper;

    @Resource
    private WeightMapper weightMapper;

    @Resource
    private LlmHandler llmHandler;

    public List<Recommend> recommend() {
        try {
            List<Action> recommends = behaviorMapper.selectList(new LambdaQueryWrapper<>(Action.class).orderByDesc(Base::getCreateTime));
            List<Vectors> goods = vectorsMapper.selectList(null);

            //  无用户行为
            if (recommends.isEmpty()) {
                return Collections.emptyList();
            }

            //  获取权重
            List<Weight> weights = weightMapper.selectList(null);
            Map<String, BigDecimal> weightMap = new HashMap<>();
            for (Weight weight : weights) {
                weightMap.put(weight.getName(), weight.getWeight());
            }

            List<Recommend> answer = new ArrayList<>();
            Date now = new Date();

            for (Action recommend : recommends) {

                //  时间衰减因子衰减系数取值范围(-∞，0),越接近0，衰减越弱
                double alpha = weightMap.get("time").doubleValue();
                double timeWeight = Math.exp(alpha * DateUtil.between(recommend.getCreateTime(), now, DateUnit.DAY));
                String content = null;
                double maxScore = -1.0;
                for (Vectors good : goods) {
                    double similarity = llmHandler.cosineSimilarity(JSONUtil.toList(recommend.getVector(), Double.class), JSONUtil.toList(good.getVector(), Double.class));
                    double score = BigDecimal.valueOf(similarity).multiply(weightMap.get(recommend.getBehavior())).multiply(BigDecimal.valueOf(timeWeight)).doubleValue();
                    if (score > maxScore) {
                        maxScore = score;
                        content = good.getContent();
                    }
                }
                answer.add(new Recommend(content, maxScore));
            }

            // 同name合并score，降序排列
            return answer.stream()
                    .collect(Collectors.groupingBy(
                            Recommend::getContent,
                            Collectors.summingDouble(Recommend::getScore)
                    ))
                    .entrySet()
                    .stream()
                    .map(entry -> new Recommend(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(Recommend::getScore).reversed())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("推荐算法异常", e);
            return Collections.emptyList();
        }
    }
}
