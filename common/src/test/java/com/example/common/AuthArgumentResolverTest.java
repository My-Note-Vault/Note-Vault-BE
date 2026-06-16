package com.example.common;

import com.example.common.exception.UnauthorizedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthArgumentResolverTest {

    private final AuthArgumentResolver authArgumentResolver = new AuthArgumentResolver();

    @Test
    @DisplayName("인증 정보가 없으면 UnauthorizedException을 던진다")
    void resolveArgument_withoutMemberId_throwsUnauthorizedException() throws Exception {
        MethodParameter parameter = authMemberIdParameter();
        ServletWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());

        assertThatThrownBy(() -> authArgumentResolver.resolveArgument(parameter, null, webRequest, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("인증 정보가 없습니다");
    }

    @Test
    @DisplayName("인증 정보가 있으면 memberId를 반환한다")
    void resolveArgument_withMemberId_returnsMemberId() throws Exception {
        MethodParameter parameter = authMemberIdParameter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CommonConstant.AUTHORIZED_MEMBER_ID, 1L);
        ServletWebRequest webRequest = new ServletWebRequest(request);

        Object result = authArgumentResolver.resolveArgument(parameter, null, webRequest, null);

        assertThat(result).isEqualTo(1L);
    }

    private MethodParameter authMemberIdParameter() throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod("endpoint", Long.class);
        return new MethodParameter(method, 0);
    }

    private static class TestController {
        @SuppressWarnings("unused")
        void endpoint(@AuthMemberId Long memberId) {
        }
    }
}
