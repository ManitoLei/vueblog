package cn.xej.shiro;

import cn.hutool.json.JSONUtil;
import cn.xej.common.lang.Result;
import cn.xej.util.JwtUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.jsonwebtoken.Claims;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtFilter extends AuthenticatingFilter {
    @Autowired
    JwtUtils jwtUtils;

    @Override
    protected AuthenticationToken createToken(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception {
        System.out.println("1111111111111");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String jwt = request.getHeader("Authorization");
        if (StringUtils.isEmpty(jwt)){
            return null;
        }

        return new JwtToken(jwt);
    }

    /**
     * 是否是拒绝登录
     * 没有登录的情况下会走此方法
     * @param servletRequest
     * @param servletResponse
     * @return
     * @throws Exception
     */
    @Override
    protected boolean onAccessDenied(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception {
        System.out.println("2222222222");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String jwt = request.getHeader("Authorization");
        if (StringUtils.isEmpty(jwt)){
            return true;
        }else {
            // 校验jwt
            Claims claim = jwtUtils.getClaimByToken(jwt);
            if(claim == null || jwtUtils.isTokenExpired(claim.getExpiration())){
                throw new ExpiredCredentialsException("token已失效，请重新登录");
            }
            // 登录
            return executeLogin(servletRequest,servletResponse);
        }
    }


    /**
     * 执行登录异常时的处理
     */
    @Override
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        System.out.println("333333333333333333");
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        Throwable throwable = e.getCause() == null ? e : e.getCause();
        Result result = Result.fail(throwable.getMessage());

        String json = JSONUtil.toJsonStr(result);
        try {
            httpServletResponse.getWriter().print(json);
        } catch (IOException e1) {

        }
        return false;
    }

    /**
     * 请求到拦截器之前解决跨域
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @Override
    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest httpServletRequest = WebUtils.toHttp(request);
        HttpServletResponse httpServletResponse = WebUtils.toHttp(response);
        httpServletResponse.setHeader("Access-control-Allow-Origin", httpServletRequest.getHeader("Origin"));
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS,PUT,DELETE");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", httpServletRequest.getHeader("Access-Control-Request-Headers"));
        // 跨域时会首先发送一个OPTIONS请求，这里我们给OPTIONS请求直接返回正常状态
        if (httpServletRequest.getMethod().equals(RequestMethod.OPTIONS.name())) {
            httpServletResponse.setStatus(org.springframework.http.HttpStatus.OK.value());
            return false;
        }

        return super.preHandle(request, response);
    }
}
