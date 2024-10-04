package cn.atsukoruo.invendtoryservice.configuration;

import io.seata.core.context.RootContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcInterceptorConfiguration implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new XidInterceptor());
    }
}

@Slf4j
class XidInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {

        String xid = RootContext.getXID();
        String rpcXid = request.getHeader(RootContext.KEY_XID);
        log.info("xid in RootContext " + xid + ",  rpcXid in RpcContext " + rpcXid);

        if (xid == null && rpcXid != null) {
            RootContext.bind(rpcXid);
            log.info("bind " + rpcXid + " to RootContext");
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception e) {
        String rpcXid = request.getHeader(RootContext.KEY_XID);

        if (StringUtils.isEmpty(rpcXid)) {
            return;
        }

        String unbindXid = RootContext.unbind();
        if (log.isDebugEnabled()) {
            log.debug("unbind {} from RootContext", unbindXid);
        }
        if (!rpcXid.equalsIgnoreCase(unbindXid)) {
            log.info("xid in change during RPC from {} to {}", rpcXid, unbindXid);
            if (unbindXid != null) {
                RootContext.bind(unbindXid);
                log.info("bind {} back to RootContext", unbindXid);
            }
        }
    }
}