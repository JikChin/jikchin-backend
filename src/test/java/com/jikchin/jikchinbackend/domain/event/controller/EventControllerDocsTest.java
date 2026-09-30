package com.jikchin.jikchinbackend.domain.event.controller;

import static com.jikchin.jikchinbackend.domain.event.controller.EventDocsFields.event;
import static com.jikchin.jikchinbackend.domain.event.controller.EventDocsFields.eventFields;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jikchin.jikchinbackend.docs.RestDocsSupport;
import com.jikchin.jikchinbackend.domain.event.service.EventService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventControllerDocsTest extends RestDocsSupport {

  private final EventService eventService = mock(EventService.class);

  @Override
  protected Object initController() {
    return new EventController(eventService);
  }

  @Test
  void getEvents() throws Exception {
    given(eventService.getEvents())
        .willReturn(
            List.of(
                event(1L, LocalDateTime.of(2026, 4, 1, 18, 30)),
                event(2L, LocalDateTime.of(2026, 4, 2, 18, 30))));

    mockMvc
        .perform(get("/api/events"))
        .andExpect(status().isOk())
        .andDo(document("event-list", successListResponse("경기 목록", eventFields())));
  }

  @Test
  void getEventsBySportAndPeriod() throws Exception {
    LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
    LocalDateTime to = LocalDateTime.of(2026, 4, 30, 23, 59, 59);
    given(eventService.getEventsBySportAndPeriod(1L, from, to, 2))
        .willReturn(
            List.of(
                event(1L, LocalDateTime.of(2026, 4, 1, 18, 30)),
                event(2L, LocalDateTime.of(2026, 4, 2, 18, 30))));

    mockMvc
        .perform(
            get("/api/events")
                .param("sportId", "1")
                .param("from", "2026-04-01T00:00:00")
                .param("to", "2026-04-30T23:59:59")
                .param("size", "2"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "event-list-by-sport-period",
                queryParameters(
                    parameterWithName("sportId").description("종목 ID"),
                    parameterWithName("from").description("조회 시작 시각 (ISO-8601, 포함)"),
                    parameterWithName("to").description("조회 종료 시각 (ISO-8601, 미포함)"),
                    parameterWithName("size").optional().description("최대 건수 (1~100, 기본 20)")),
                successListResponse("경기 목록 (시작 시각순)", eventFields())));
  }

  @Test
  void getEvent() throws Exception {
    given(eventService.getEvent(1L)).willReturn(event(1L, LocalDateTime.of(2026, 4, 1, 18, 30)));

    mockMvc
        .perform(get("/api/events/{eventId}", 1L))
        .andExpect(status().isOk())
        .andDo(
            document(
                "event-get",
                pathParameters(parameterWithName("eventId").description("경기 ID")),
                successResponse(eventFields())));
  }
}
