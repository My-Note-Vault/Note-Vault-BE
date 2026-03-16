package com.example.common;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * common은 라이브러리 모듈이므로 독립적인 Context 로딩 테스트가 불가능합니다.
 * 실제 Context 로딩은 루트 프로젝트에서 테스트됩니다.
 */
@Disabled("라이브러리 모듈 - Context 로딩 테스트 불필요")
class CommonApplicationTests {

    @Test
    void contextLoads() {
    }

}
