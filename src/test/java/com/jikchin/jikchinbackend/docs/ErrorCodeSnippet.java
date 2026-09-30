package com.jikchin.jikchinbackend.docs;

import com.jikchin.jikchinbackend.global.error.ErrorType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.restdocs.operation.Operation;
import org.springframework.restdocs.snippet.TemplatedSnippet;

/**
 * {@link ErrorType} 전체를 에러 코드 표 스니펫(error-codes.adoc)으로 만든다.
 *
 * <p>요청/응답과 무관한 표지만 TemplatedSnippet으로 만들어, 출력 경로·인코딩·표 셀 이스케이프를 REST Docs 설정에 맡긴다. 템플릿은
 * src/test/resources/org/springframework/restdocs/templates/asciidoctor/error-codes.snippet 이다.
 */
public class ErrorCodeSnippet extends TemplatedSnippet {

  public ErrorCodeSnippet() {
    super("error-codes", null);
  }

  @Override
  protected Map<String, Object> createModel(Operation operation) {
    List<Map<String, Object>> errors =
        Arrays.stream(ErrorType.values())
            .map(
                type ->
                    Map.<String, Object>of(
                        "code", type.getErrorCode().name(),
                        "status",
                            type.getStatus().value() + " " + type.getStatus().getReasonPhrase(),
                        "name", type.name(),
                        "message", type.getMessage()))
            .toList();
    // TemplatedSnippet.document()가 반환 모델에 attributes를 putAll하므로 변경 가능한 Map이어야 한다.
    Map<String, Object> model = new HashMap<>();
    model.put("errors", errors);
    return model;
  }
}
