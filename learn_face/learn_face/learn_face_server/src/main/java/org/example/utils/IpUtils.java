package org.example.utils;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.constant.Constant;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

/**
 * 功能描述: ip工具类
 *
 * @author zb
 * @date 2024/3/9
 * @return
 */
@Slf4j
public class IpUtils {

    /**
     * 功能描述: 获取本地ip地址
     *
     * @param
     * @return java.lang.String
     */
    private static String getLocalIp() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String localIp = localHost.getHostAddress();
            log.info("IpUtils.getLocalIp:{}", localIp);
            return localIp;
        } catch (Exception e) {
            log.error("IpUtils.getLocalIp.error:{}", e.getMessage(), e);
            return Constant.DEFAULT_IP;
        }
    }

    /**
     * 功能描述: 获取ip地址
     *
     * @param request
     * @return java.lang.String
     */
    public static String getIp(HttpServletRequest request) {
        String ip = null;
        try {
            ip = request.getHeader("x-forwarded-for");
            if (StrUtil.isEmpty(ip) || Constant.IP_UN_KNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (StrUtil.isEmpty(ip) || ip.isEmpty() || Constant.IP_UN_KNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (StrUtil.isEmpty(ip) || Constant.IP_UN_KNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (StrUtil.isEmpty(ip) || Constant.IP_UN_KNOWN.equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (StrUtil.isEmpty(ip) || Constant.IP_UN_KNOWN.equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (StrUtil.isEmpty(ip)) {
            assert ip != null;
            if (ip.length() > Constant.IP_MAX_LENGTH) {
                if (ip.indexOf(Constant.IP_SPLIT) > 0) {
                    ip = ip.substring(0, ip.indexOf(Constant.IP_SPLIT));
                }
            }
        }
        return ip;
    }

    /**
     * 功能描述: 获取远程地址
     *
     * @param request
     * @return java.lang.String
     */
    public static String getRemoteHost(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.contains("0:0:0:0:0:0:0:1") ? "127.0.0.1" : ip;
    }
}