package com.jikchin.jikchinbackend.docs;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

import com.jikchin.jikchinbackend.global.exception.GlobalExceptionHandler;
import com.jikchin.jikchinbackend.global.security.MemberAuthenticationToken;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * REST Docs 문서화 테스트의 공통 설정.
 *
 * <p>스프링 컨텍스트 없이 컨트롤러 하나만 올리는 standalone MockMvc를 쓴다. 문서 테스트는 요청/응답의 모양만 책임지고, 인증·인가 흐름은 통합 테스트(예:
 * MateAuthenticationIntegrationTest)가 검증한다.
 */
@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsSupport {

  protected MockMvc mockMvc;

  @BeforeEach
  void setUpMockMvc(RestDocumentationContextProvider provider) {
    mockMvc =
        MockMvcBuilders.standaloneSetup(initController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .apply(
                documentationConfiguration(provider)
                    .operationPreprocessors()
                    .withRequestDefaults(prettyPrint())
                    .withResponseDefaults(prettyPrint()))
            .build();
  }

  /** memberAuth()가 스레드의 SecurityContextHolder에 직접 넣은 인증을 다음 테스트로 새지 않게 비운다. */
  @AfterEach
  void clearSecurityContext() {
    TestSecurityContextHolder.clearContext();
  }

  protected abstract Object initController();

  /**
   * 요청을 memberKey 회원으로 인증된 상태로 만든다.
   *
   * <p>spring-security-test의 authentication()은 SecurityContext를 요청의 저장소에만 두고, 이를
   * SecurityContextHolder로 옮기는 일은 springSecurity() 필터 체인이 맡는다. 필터가 없는 standalone MockMvc에서는
   * {@code @AuthenticationPrincipal}이 null이 되므로, 디스패치 직전에 같은 스레드의 SecurityContextHolder에 직접 넣는다.
   */
  protected static RequestPostProcessor memberAuth(UUID memberKey) {
    return request -> {
      TestSecurityContextHolder.setAuthentication(
          new MemberAuthenticationToken(memberKey, List.of()));
      return request;
    };
  }

  /** 모든 성공 응답이 공유하는 ApiResponse 껍데기와 data 하위 필드. */
  protected static ResponseFieldsSnippet successResponse(FieldDescriptor... dataFields) {
    return responseFields(
            fieldWithPath("resultType")
                .type(JsonFieldType.STRING)
                .description("결과 타입. 성공 시 SUCCESS"),
            fieldWithPath("error").type(JsonFieldType.NULL).description("성공 시 null"))
        .andWithPrefix("data.", dataFields);
  }
}
