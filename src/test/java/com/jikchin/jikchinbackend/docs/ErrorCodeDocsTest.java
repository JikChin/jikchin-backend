package com.jikchin.jikchinbackend.docs;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** ErrorType 전체를 에러 코드 표로 문서화한다. 스니펫을 만들 계기가 될 요청만 필요해 스텁 컨트롤러를 쓴다. */
class ErrorCodeDocsTest extends RestDocsSupport {

  @Override
  protected Object initController() {
    return new ErrorCodeController();
  }

  @Test
  void errorCodes() throws Exception {
    mockMvc
        .perform(get("/docs/error-codes"))
        .andExpect(status().isOk())
        .andDo(document("error-code", new ErrorCodeSnippet()));
  }

  @RestController
  static class ErrorCodeController {

    @GetMapping("/docs/error-codes")
    ResponseEntity<Void> errorCodes() {
      return ResponseEntity.ok().build();
    }
  }
}
