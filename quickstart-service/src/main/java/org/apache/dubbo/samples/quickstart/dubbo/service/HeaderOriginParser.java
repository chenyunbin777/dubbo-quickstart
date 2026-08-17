package org.apache.dubbo.samples.quickstart.dubbo.service;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.RequestOriginParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * @author chenyunbin
 * @date 2026/8/5 上午9:59
 */
@Component
public class HeaderOriginParser implements RequestOriginParser {

    @Override
    public String parseOrigin(HttpServletRequest request) {
        // 从请求头获取 origin 值，若没有则默认返回 "unknown"
        String origin = request.getHeader("origin");
        return origin != null ? origin : "unknown";
    }
}
