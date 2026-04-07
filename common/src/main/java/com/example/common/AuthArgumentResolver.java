package com.example.common;

import com.example.common.exception.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AuthArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthMemberId.class)
                && (parameter.getParameterType().equals(Long.class)
                || parameter.getParameterType().equals(long.class));
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        Object memberId = webRequest.getAttribute(CommonConstant.AUTHORIZED_MEMBER_ID, RequestAttributes.SCOPE_REQUEST);

        if (memberId == null) {
            throw new UnauthorizedException("인증 정보가 없습니다");
        }

        try {
            return memberId;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Member-Id header value: " + memberId);
        }
    }
}
