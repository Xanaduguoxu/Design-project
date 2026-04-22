package org.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.example.config.security.LoginUser;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JwtUtils {

    //  私钥
    private static final String TOKEN_SECRET = "privateKey";

    //  token过期时间-30天
    public static final long EXPIRE_TIME = 30 * 24 * 60 * 60 * 1000L;


    public static String sign(Map<String, Object> loadInfoMap) {
        try {
            //  设置过期时间
            Date expireTime = new Date(System.currentTimeMillis() + EXPIRE_TIME);
            //  私钥和加密算法
            Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
            //  设置头部信息
            Map<String, Object> header = new HashMap<>(2);
            header.put("typ", "jwt");
            // 返回token字符串
            JWTCreator.Builder builder = JWT.create().withHeader(header)
                    //  签发时间
                    .withIssuedAt(new Date())
                    //  过期时间
                    .withExpiresAt(expireTime);

            //  参数传递
            loadInfoMap.forEach((key, value) -> {
                if (value instanceof Integer) {
                    builder.withClaim(key, (Integer) value);
                } else if (value instanceof Long) {
                    builder.withClaim(key, (Long) value);
                } else if (value instanceof Boolean) {
                    builder.withClaim(key, (Boolean) value);
                } else if (value instanceof String) {
                    builder.withClaim(key, String.valueOf(value));
                } else if (value instanceof Double) {
                    builder.withClaim(key, (Double) value);
                } else if (value instanceof Date) {
                    builder.withClaim(key, (Date) value);
                }
            });

            return builder.sign(algorithm);
        } catch (Exception e) {
            return null;
        }
    }


    public static LoginUser parseToken(String token) {
        try {
            // 使用相同的密钥和算法来验证token
            Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            // 解码并验证token
            DecodedJWT jwt = verifier.verify(token);
            // 获取token中的claims
            Map<String, Claim> claims = jwt.getClaims();

            LoginUser loginUser = new LoginUser();
            loginUser.setId(claims.get("id").asLong());
            loginUser.setUsername(claims.get("username").asString());
            loginUser.setPassword(claims.get("password").asString());
            loginUser.setRole(claims.get("role").asString());
            loginUser.setPermission(Collections.singletonList(claims.get("role").asString()));

            return loginUser;
        } catch (JWTVerificationException exception) {
            // 如果token验证失败，会抛出异常
            System.out.println("Invalid token: " + exception.getMessage());
            return null;
        }
    }

}
