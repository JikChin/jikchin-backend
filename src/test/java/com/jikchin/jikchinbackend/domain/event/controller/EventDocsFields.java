package com.jikchin.jikchinbackend.domain.event.controller;

import static com.jikchin.jikchinbackend.docs.RestDocsSupport.enumValues;
import static com.jikchin.jikchinbackend.docs.RestDocsSupport.withPrefix;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

import com.jikchin.jikchinbackend.domain.event.dto.response.EventResponse;
import com.jikchin.jikchinbackend.domain.event.dto.response.EventResponse.TeamResponse;
import com.jikchin.jikchinbackend.domain.event.dto.response.EventResponse.VenueResponse;
import com.jikchin.jikchinbackend.domain.event.entity.EventStatus;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.restdocs.payload.FieldDescriptor;

/** EventController와 AdminEventController 문서가 함께 쓰는 EventResponse 필드와 픽스처. */
final class EventDocsFields {

  private EventDocsFields() {}

  static FieldDescriptor[] eventFields() {
    List<FieldDescriptor> fields = new ArrayList<>();
    fields.add(fieldWithPath("id").type(NUMBER).description("경기 ID"));
    fields.add(fieldWithPath("sport").type(STRING).description("종목 이름"));
    fields.add(fieldWithPath("leagueName").type(STRING).description("리그 이름"));
    fields.add(fieldWithPath("home").type(OBJECT).description("홈 팀"));
    fields.addAll(List.of(withPrefix("home.", teamFields())));
    fields.add(fieldWithPath("away").type(OBJECT).description("원정 팀"));
    fields.addAll(List.of(withPrefix("away.", teamFields())));
    fields.add(fieldWithPath("venue").type(OBJECT).description("경기장"));
    fields.addAll(List.of(withPrefix("venue.", venueFields())));
    fields.add(fieldWithPath("startsAt").type(STRING).description("경기 시작 시각 (ISO-8601)"));
    fields.add(
        fieldWithPath("status")
            .type(STRING)
            .description("경기 상태: " + enumValues(EventStatus.class)));
    return fields.toArray(FieldDescriptor[]::new);
  }

  static EventResponse event(Long id, LocalDateTime startsAt) {
    return new EventResponse(
        id,
        "야구",
        "KBO 리그",
        new TeamResponse(1L, "LG 트윈스", "LG", "https://example.com/logos/lg.png"),
        new TeamResponse(2L, "두산 베어스", "두산", "https://example.com/logos/doosan.png"),
        new VenueResponse(1L, "잠실야구장", "서울", "서울특별시 송파구 올림픽로 25"),
        startsAt,
        EventStatus.SCHEDULED);
  }

  private static FieldDescriptor[] teamFields() {
    return new FieldDescriptor[] {
      fieldWithPath("id").type(NUMBER).description("팀 ID"),
      fieldWithPath("name").type(STRING).description("팀 이름"),
      fieldWithPath("shortName").type(STRING).optional().description("팀 약칭"),
      fieldWithPath("logoUrl").type(STRING).optional().description("로고 이미지 URL")
    };
  }

  private static FieldDescriptor[] venueFields() {
    return new FieldDescriptor[] {
      fieldWithPath("id").type(NUMBER).description("경기장 ID"),
      fieldWithPath("name").type(STRING).description("경기장 이름"),
      fieldWithPath("region").type(STRING).description("지역"),
      fieldWithPath("address").type(STRING).optional().description("주소")
    };
  }
}
