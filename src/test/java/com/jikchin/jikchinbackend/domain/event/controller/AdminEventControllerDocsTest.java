package com.jikchin.jikchinbackend.domain.event.controller;

import static com.jikchin.jikchinbackend.domain.event.controller.EventDocsFields.event;
import static com.jikchin.jikchinbackend.domain.event.controller.EventDocsFields.eventFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jikchin.jikchinbackend.docs.RestDocsSupport;
import com.jikchin.jikchinbackend.domain.event.dto.request.EventCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.request.SportCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.request.TeamCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.request.VenueCreateRequest;
import com.jikchin.jikchinbackend.domain.event.dto.response.CatalogResponse;
import com.jikchin.jikchinbackend.domain.event.entity.SportCode;
import com.jikchin.jikchinbackend.domain.event.service.EventService;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;

class AdminEventControllerDocsTest extends RestDocsSupport {

  // 요청 DTO의 startsAt은 @Future를 실제 시계로 검증하므로, 테스트가 해가 지나도 깨지지 않게 내년 날짜를 쓴다.
  private static final LocalDateTime STARTS_AT =
      LocalDateTime.of(Year.now().getValue() + 1, 4, 1, 18, 30);

  private final EventService eventService = mock(EventService.class);

  @Override
  protected Object initController() {
    return new AdminEventController(eventService);
  }

  @Test
  void createEvent() throws Exception {
    given(eventService.create(any(EventCreateRequest.class))).willReturn(event(1L, STARTS_AT));

    mockMvc
        .perform(
            post("/api/admin/events")
                .header(HttpHeaders.AUTHORIZATION, ADMIN_ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sportId":1,"venueId":1,"homeTeamId":1,"awayTeamId":2,\
                    "leagueName":"KBO 리그","startsAt":"%s"}
                    """
                        .formatted(STARTS_AT.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin-event-create",
                adminAuthorizationHeader(),
                requestFields(
                    fieldWithPath("sportId").type(NUMBER).description("종목 ID"),
                    fieldWithPath("venueId").type(NUMBER).description("경기장 ID"),
                    fieldWithPath("homeTeamId").type(NUMBER).description("홈 팀 ID"),
                    fieldWithPath("awayTeamId").type(NUMBER).description("원정 팀 ID"),
                    fieldWithPath("leagueName").type(STRING).description("리그 이름"),
                    fieldWithPath("startsAt")
                        .type(STRING)
                        .description("경기 시작 시각 (ISO-8601, 현재 이후)")),
                successResponse(eventFields())));
  }

  @Test
  void createSport() throws Exception {
    given(eventService.createSport(any(SportCreateRequest.class)))
        .willReturn(new CatalogResponse(1L, "야구"));

    mockMvc
        .perform(
            post("/api/admin/events/catalog/sports")
                .header(HttpHeaders.AUTHORIZATION, ADMIN_ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"BASEBALL","name":"야구"}
                    """))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin-sport-create",
                adminAuthorizationHeader(),
                requestFields(
                    fieldWithPath("code")
                        .type(STRING)
                        .description("종목 코드: " + enumValues(SportCode.class)),
                    fieldWithPath("name").type(STRING).description("종목 이름")),
                successResponse(catalogFields("종목"))));
  }

  @Test
  void createTeam() throws Exception {
    given(eventService.createTeam(any(TeamCreateRequest.class)))
        .willReturn(new CatalogResponse(1L, "LG 트윈스"));

    mockMvc
        .perform(
            post("/api/admin/events/catalog/teams")
                .header(HttpHeaders.AUTHORIZATION, ADMIN_ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sportId":1,"name":"LG 트윈스","shortName":"LG",\
                    "logoUrl":"https://example.com/logos/lg.png"}
                    """))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin-team-create",
                adminAuthorizationHeader(),
                requestFields(
                    fieldWithPath("sportId").type(NUMBER).description("팀이 속한 종목 ID"),
                    fieldWithPath("name").type(STRING).description("팀 이름"),
                    fieldWithPath("shortName").type(STRING).optional().description("팀 약칭"),
                    fieldWithPath("logoUrl").type(STRING).optional().description("로고 이미지 URL")),
                successResponse(catalogFields("팀"))));
  }

  @Test
  void createVenue() throws Exception {
    given(eventService.createVenue(any(VenueCreateRequest.class)))
        .willReturn(new CatalogResponse(1L, "잠실야구장"));

    mockMvc
        .perform(
            post("/api/admin/events/catalog/venues")
                .header(HttpHeaders.AUTHORIZATION, ADMIN_ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"잠실야구장","region":"서울","address":"서울특별시 송파구 올림픽로 25"}
                    """))
        .andExpect(status().isOk())
        .andDo(
            document(
                "admin-venue-create",
                adminAuthorizationHeader(),
                requestFields(
                    fieldWithPath("name").type(STRING).description("경기장 이름"),
                    fieldWithPath("region").type(STRING).description("지역"),
                    fieldWithPath("address").type(STRING).optional().description("주소")),
                successResponse(catalogFields("경기장"))));
  }

  private FieldDescriptor[] catalogFields(String label) {
    return new FieldDescriptor[] {
      fieldWithPath("id").type(NUMBER).description("생성된 " + label + " ID"),
      fieldWithPath("name").type(STRING).description(label + " 이름")
    };
  }
}
