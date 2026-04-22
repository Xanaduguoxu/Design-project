package org.example.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.annotation.Behavior;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.config.security.LoginUser;
import org.example.constant.Result;
import org.example.handler.LlmHandler;
import org.example.mapper.algorithm.BehaviorMapper;
import org.example.mapper.algorithm.VectorsMapper;
import org.example.mapper.common.LogMapper;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.algorithm.Action;
import org.example.pojo.po.algorithm.Vectors;
import org.example.pojo.po.common.Log;
import org.example.utils.BeanUtils;
import org.example.utils.IpUtils;
import org.example.utils.JwtUtils;
import org.example.utils.ThreadLocalUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 功能描述: 日志操作记录AOP
 */
@Slf4j
@Aspect
@Configuration
@ConditionalOnProperty(name = {"log.aspect.enable"}, havingValue = "true", matchIfMissing = true)
public class AopAspect {

    public final static String TOKEN_NAME = "Authorization";

    @Resource
    private LogMapper logMapper;

    @Resource
    private BehaviorMapper behaviorMapper;

    @Resource
    private VectorsMapper vectorsMapper;

    @Resource
    private LlmHandler llmHandler;


    /**
     * 功能描述: 切点
     *
     * @param
     * @return void
     */
    @Pointcut("execution(* org.example.controller..*Controller.*(..))")
    public void webPointCut() {
    }

    /**
     * 功能描述: 环绕通知
     *
     * @param proceedingJoinPoint
     * @return java.lang.Object
     */
    @Around("webPointCut()")
    public Object doAroundAdvice(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        Object[] requestParams = proceedingJoinPoint.getArgs();
        List<Object> params = new LinkedList<>();

        for (Object requestParam : requestParams) {
            if (requestParam instanceof HttpServletRequest || requestParam instanceof HttpServletResponse || requestParam instanceof MultipartFile) {
                continue;
            }
            params.add(requestParam);
        }

        HttpServletRequest request = BeanUtils.getHttpServletRequest();

        if (Objects.equals("/log/list", request.getServletPath())) {
            return proceedingJoinPoint.proceed();
        }

        String token = request.getHeader(TOKEN_NAME);

        String account = null;
        if (ObjectUtil.isNull(token)) {
            if (Objects.equals("/auth/login", request.getServletPath())) {
                if (params.get(0) instanceof JSONObject) {
                    account = ((JSONObject) params.get(0)).getStr("username");
                }
            }
        } else {
            LoginUser loginUser = JwtUtils.parseToken(token);
            assert loginUser != null;
            account = loginUser.getUsername();
        }

        // 获取Log注解信息
        Signature signature = proceedingJoinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        Method method = methodSignature.getMethod();
        String operation = "方法上面没有添加@Logs注解！";
        if (method.getAnnotation(Logs.class) != null) {
            operation = method.getAnnotation(Logs.class).value();
        }

        Log log = Log.builder().username(account).operation(operation).args(JSONUtil.toJsonStr(params)).url(request.getServletPath()).ip(IpUtils.getIp(request)).build();
        logMapper.insert(log);

        return proceedingJoinPoint.proceed();
    }

    @Around("@annotation(behavior)")
    public Object behavior(ProceedingJoinPoint joinPoint, Behavior behavior) throws Throwable {
        Object[] requestParams = joinPoint.getArgs();
        List<Object> params = new LinkedList<>();
        for (Object requestParam : requestParams) {
            if (requestParam instanceof HttpServletRequest || requestParam instanceof HttpServletResponse || requestParam instanceof MultipartFile) {
                continue;
            }
            params.add(requestParam);
        }

        JSONObject bean = JSONUtil.toBean(JSONUtil.toJsonStr(params.get(0)), JSONObject.class);
        String value = behavior.value();
        switch (value) {
            case "add": {
                List<Double> embedding = llmHandler.embedding(bean.getStr("name"));
                Vectors vectors = new Vectors(bean.getStr("name"), JSONUtil.toJsonStr(bean), JSONUtil.toJsonStr(embedding));
                vectorsMapper.insert(vectors);
                Long id = vectors.getId();
                ThreadLocalUtils.set(id);
                break;
            }
            case "del": {
                Long id = Long.valueOf(params.get(0).toString());
                vectorsMapper.deleteById(id);
                break;
            }
            case "update": {
                List<Double> embedding = llmHandler.embedding(bean.getStr("name"));
                Vectors vectors = new Vectors(bean.getStr("name"), JSONUtil.toJsonStr(bean), JSONUtil.toJsonStr(embedding));
                vectors.setId(bean.getLong("id"));
                vectorsMapper.updateById(vectors);
                break;
            }
            case "list": {
                String keyword = bean.getStr("keyword");
                if (StrUtil.isNotBlank(keyword)) {
                    List<Double> embedding = llmHandler.embedding(keyword);
                    behaviorMapper.insert(new Action(keyword, value, JSONUtil.toJsonStr(embedding)));
                }
                break;
            }
            case "click": {
                Long id = Long.valueOf(params.get(0).toString());
                Vectors vectors = vectorsMapper.selectById(id);
                String name = vectors.getName();
                List<Double> embedding = llmHandler.embedding(name);
                behaviorMapper.insert(new Action(name, value, JSONUtil.toJsonStr(embedding)));
                break;
            }
        }

        return joinPoint.proceed();
    }

    @Around("@annotation(exclude)")
    public Object exclude(ProceedingJoinPoint joinPoint, Exclude exclude) throws Throwable {
        Object result = joinPoint.proceed();
        if (result == null) {
            return null;
        }
        String[] excludeFields = exclude.value();

        switch (exclude.type()) {
            case "details":
                return processDetails((Result<JSONObject>) result, excludeFields);
            case "page":
                return processPage((Result<PageVo>) result, excludeFields);
            case "list":
                return processList((Result<List<JSONObject>>) result, excludeFields);
            default:
                return result;
        }
    }

    private Result<JSONObject> processDetails(Result<JSONObject> result, String[] excludeFields) {
        JSONObject data = result.getData();
        if (data != null) {
            handleJson(data, excludeFields);
        }
        return result;
    }

    private Result<PageVo> processPage(Result<PageVo> result, String[] excludeFields) {
        PageVo pageVo = result.getData();
        if (pageVo == null || pageVo.getData() == null) {
            return result;
        }

        Object data = pageVo.getData();
        if (data instanceof Collection) {
            List<JSONObject> list = new ArrayList<>();
            for (Object o : (Collection<?>) data) {
                JSONObject json = JSONUtil.parseObj(o);
                handleJson(json, excludeFields);
                list.add(json);
            }
            pageVo.setData(list);
        }
        return result;
    }

    private Result<List<JSONObject>> processList(Result<List<JSONObject>> result, String[] excludeFields) {
        List<JSONObject> dataList = result.getData();
        if (dataList != null) {
            dataList.forEach(json -> handleJson(json, excludeFields));
        }
        return result;
    }

    private void handleJson(JSONObject json, String[] excludeFields) {
        // 移除指定字段
        for (String field : excludeFields) {
            json.remove(field);
        }
        // id 转 String（防止前端精度丢失）
        if (json.containsKey("id")) {
            json.set("id", json.getStr("id"));
        }

        // 自动格式化时间类型字段
        for (String key : new ArrayList<>(json.keySet())) {
            Object value = json.get(key);
            if (value instanceof Date) {
                json.set(key, DateUtil.format((Date) value, "yyyy-MM-dd HH:mm:ss"));
            }
        }
    }
}
