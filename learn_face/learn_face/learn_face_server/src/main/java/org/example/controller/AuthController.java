package org.example.controller;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.annotation.Exclude;
import org.example.annotation.Logs;
import org.example.config.security.LoginUser;
import org.example.config.security.SecurityUtils;
import org.example.constant.Result;
import org.example.handler.EmailHandler;
import org.example.mapper.MedalLogMapper;
import org.example.mapper.MedalMapper;
import org.example.mapper.UserMapper;
import org.example.mapper.common.AuthMapper;
import org.example.pojo.base.PageVo;
import org.example.pojo.po.Medal;
import org.example.pojo.po.MedalLog;
import org.example.pojo.po.User;
import org.example.pojo.po.common.Auth;
import org.example.utils.JwtUtils;
import org.example.utils.ReqUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthMapper authMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private MedalMapper medalMapper;

    @Resource
    private MedalLogMapper medalLogMapper;

    @Resource
    private EmailHandler emailHandler;

    // 创建定时缓存，过期时间10分钟
    private static final TimedCache<String, String> CODE_CACHE = CacheUtil.newTimedCache(10 * 60 * 1000);

    @Logs("登陆")
    @Exclude(type = "details")
    @PostMapping("/login")
    public Result<JSONObject> login(@RequestBody JSONObject req) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(req.getStr("username"), req.getStr("password"));
        Authentication authenticate = authenticationManager.authenticate(authentication);

        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        Map<String, Object> loginUserMap = BeanUtil.beanToMap(loginUser);
        String token = JwtUtils.sign(loginUserMap);

        User user = userMapper.selectById(loginUser.getId());
        Map<String, Object> userMap = BeanUtil.beanToMap(user, false, false);

        userMap.replaceAll((key, value) -> value == null ? "" : value);
        JSONObject jsonObject = new JSONObject(userMap);
        jsonObject.putOpt("token", token);
        return Result.success(jsonObject);
    }

    @Exclude(type = "details")
    @PostMapping("/faceLogin")
    public Result<JSONObject> faceLogin(@RequestBody JSONObject req) {
        Map<String, Object> param = new HashMap<>();
        param.put("image_base64", req.getStr("imageBase64"));

        String resp = HttpUtil.post("http://localhost:8000/api/face/recognize", JSONUtil.toJsonStr(param));
        JSONObject bean = JSONUtil.toBean(resp, JSONObject.class);

        User user = userMapper.selectById(bean.getLong("user_id"));

        LoginUser loginUser = new LoginUser();

        BeanUtil.copyProperties(user, loginUser);

        loginUser.setExpired(0);
        loginUser.setCredentials(0);
        loginUser.setLocked(0);
        loginUser.setEnabled(0);

        Map<String, Object> loginUserMap = BeanUtil.beanToMap(loginUser);
        String token = JwtUtils.sign(loginUserMap);

        // 4. 返回前端
        Map<String, Object> userMap = BeanUtil.beanToMap(user, false, false);
        userMap.replaceAll((key, value) -> value == null ? "" : value);
        JSONObject jsonObject = new JSONObject(userMap);
        jsonObject.putOpt("token", token);

        return Result.success(jsonObject);
    }

    @PostMapping("/faceRegister")
    public Result<Boolean> faceRegister(@RequestBody JSONObject req) {
        Map<String, Object> param = new HashMap<>();
        param.put("images_base64", req.getBeanList("imagesBase64", String.class));
        param.put("user_id", SecurityUtils.getUserId());
        String resp = HttpUtil.post("http://localhost:8000/api/face/register", JSONUtil.toJsonStr(param));
        System.out.println(resp);

        return Result.success(true);
    }

    @Logs("注册")
    @Transactional
    @PostMapping("/register")
    public Result<Boolean> register(@RequestBody JSONObject req) {
        checkUsernameUnique(req.getStr("username"));

        Auth auth = new Auth();
        BeanUtil.copyProperties(req, auth);
        auth.setRole("user");
        auth.setPassword(SecurityUtils.getPassword(req.getStr("password")));
        authMapper.insert(auth);

        User user = new User();
        BeanUtil.copyProperties(auth, user);
        user.setNickname(req.getStr("username"));
        user.setEmail(auth.getUsername());
        userMapper.insert(user);

        Medal medal = medalMapper.selectById(1);
        MedalLog medalLog = new MedalLog();
        BeanUtil.copyProperties(medal, medalLog);
        medalLog.setMedalId(medal.getId());
        medalLog.setUnlocked("true");
        medalLog.setId(null);
        medalLog.setObtainedDate(new Date());
        medalLogMapper.insert(medalLog);

        return Result.success(true);
    }

    @Transactional
    @PostMapping("/add")
    public Result<Boolean> add(@RequestBody JSONObject req) {
        checkUsernameUnique(req.getStr("username"));

        Auth auth = new Auth();
        BeanUtil.copyProperties(req, auth);
        auth.setPassword(SecurityUtils.getPassword("123456"));
        authMapper.insert(auth);

        User user = new User();
        BeanUtil.copyProperties(req, user);
        userMapper.insert(user);

        return Result.success(true);
    }

    @Logs("删除账号")
    @Transactional
    @DeleteMapping("/del")
    public Result<Boolean> del(@RequestParam("id") Long id) {
        userMapper.deleteById(id);
        authMapper.deleteById(id);
        return Result.success(true);
    }

    @Logs("修改信息")
    @Transactional
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody JSONObject req) {
        Auth auth = new Auth();
        BeanUtil.copyProperties(req, auth);
        authMapper.updateById(auth);

        User user = new User();
        BeanUtil.copyProperties(req, user);
        userMapper.updateById(user);

        return Result.success(true);
    }

    @Logs("用户列表")
    @Exclude(type = "page", value = {"password", "createBy", "createTime", "updateBy", "updateTime", "del"})
    @PostMapping("/list")
    public Result<PageVo> list(@RequestBody JSONObject req) {
        Page<User> page = new Page<>(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req));
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        String keyword = req.getStr("keyword");
        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(wrapper -> wrapper.like(User::getNickname, keyword).or().like(User::getUsername, keyword).or().like(User::getGender, keyword));
        }

        userMapper.selectPage(page, queryWrapper);
        PageVo answer = new PageVo(ReqUtils.getCurrentPage(req), ReqUtils.getPageSize(req), page.getTotal(), page.getRecords());
        return Result.success(answer);
    }

    @Logs("修改密码")
    @Transactional
    @PostMapping("/password")
    public Result<Boolean> password(@RequestBody JSONObject req) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(req.getStr("username"), req.getStr("password"));
        Authentication authenticate = authenticationManager.authenticate(authentication);

        String newPassword = SecurityUtils.getPassword(req.getStr("newPassword"));

        LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
        Auth auth = new Auth();
        auth.setId(loginUser.getId());
        auth.setPassword(newPassword);
        authMapper.updateById(auth);

        User userInfo = new User();
        userInfo.setId(loginUser.getId());
        userInfo.setPassword(newPassword);
        userMapper.updateById(userInfo);
        return Result.success(true);
    }

    @Logs("发送验证码")
    @GetMapping("/sendCode")
    public Result<Boolean> sendCode(@RequestParam("email") String email) {
        String content = "验证码：【code】（验证码仅用于您本人找回密码）。";
        String code = RandomUtil.randomNumbers(6);
        //  发送人
        String sendContent = content.replace("code", code);
        CODE_CACHE.put(email, code);
        return Result.success(emailHandler.sendOfContext(email, sendContent));
    }

    @Logs("找回密码")
    @Transactional
    @PostMapping("/findPassword")
    public Result<Boolean> findPassword(@RequestBody JSONObject req) {
        String code = CODE_CACHE.get(req.getStr("email"));
        if (req.getStr("code").equals(code)) {
            String newPassword = SecurityUtils.getPassword(req.getStr("newPassword"));
            Auth auth = new Auth();
            auth.setPassword(newPassword);
            authMapper.update(auth, new LambdaQueryWrapper<>(Auth.class).eq(Auth::getUsername, req.getStr("email")));

            User userInfo = new User();
            userInfo.setPassword(newPassword);
            userMapper.update(userInfo, new LambdaQueryWrapper<>(User.class).eq(User::getUsername, req.getStr("email")));
            return Result.success(true);
        }
        return Result.success(false);
    }

    @Logs("id映射")
    @GetMapping("/mapId")
    public Result<List<JSONObject>> mapId() {
        List<JSONObject> collect = userMapper.selectList(null).stream().map(item -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("id", item.getId());
            jsonObject.putOpt("name", item.getNickname());
            return jsonObject;
        }).collect(Collectors.toList());
        return Result.success(collect);
    }

    private void checkUsernameUnique(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        Auth auth = authMapper.selectOne(new LambdaQueryWrapper<Auth>().eq(Auth::getUsername, username));
        if (!Objects.isNull(auth)) {
            throw new IllegalArgumentException("用户名重复!");
        }
    }
}
