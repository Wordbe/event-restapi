package co.wordbe.eventrestapi.events;

import co.wordbe.eventrestapi.accounts.Account;
import co.wordbe.eventrestapi.accounts.AccountRepository;
import co.wordbe.eventrestapi.accounts.AccountRole;
import co.wordbe.eventrestapi.accounts.AccountService;
import co.wordbe.eventrestapi.common.AppProperties;
import co.wordbe.eventrestapi.common.BaseControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.util.Jackson2JsonParser;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.IntStream;

import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class EventControllerTest extends BaseControllerTest {

    @Autowired
    EventRepository eventRepository;

    @Autowired
    AccountService accountService;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AppProperties appProperties;

    @BeforeEach
    public void setUp() {
        this.eventRepository.deleteAll();
        this.accountRepository.deleteAll();
    }


    @Test
    @DisplayName("이벤트 생성 정상 테스트")
    public void createEvent() throws Exception {
        EventDto event = EventDto.builder()
                .name("Spring")
                .description("REST API Development")
                .beginEnrollmentDateTime(LocalDateTime.of(2021,01,21,12,00,00))
                .closeEnrollmentDateTime(LocalDateTime.of(2021,01,22,12,00,00))
                .beginEventDateTime(LocalDateTime.of(2021,01,22,12,00,00))
                .endEventDateTime(LocalDateTime.of(2021,01,23,12,00,00))
                .basePrice(100)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("D2 스타트업 팩토리")
                .build();

        mockMvc.perform(post("/api/events")
                    .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaTypes.HAL_JSON)
                    .content(objectMapper.writeValueAsString(event))
                        )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("id").exists())
                .andExpect(header().exists(HttpHeaders.LOCATION))
//                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaTypes.HAL_JSON_VALUE + ";charset=UTF-8"))
                .andExpect(jsonPath("free").value(false))
                .andExpect(jsonPath("offline").value(true))
                .andExpect(jsonPath("_links.self").exists())
                .andExpect(jsonPath("_links.query-events").exists())
                .andExpect(jsonPath("_links.update-event").exists())
                .andDo(document("create-event",
                        links(
                                linkWithRel("self").description("link to self"),
                                linkWithRel("query-events").description("link to query events"),
                                linkWithRel("update-event").description("link to update event"),
                                linkWithRel("profile").description("link to profile")
                        ),
                        requestHeaders(
                                headerWithName(HttpHeaders.ACCEPT).description("accept header"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("content type header")
                        ),
                        requestFields(
                                fieldWithPath("name").description("name of event"),
                                fieldWithPath("description").description("description of event"),
                                fieldWithPath("beginEnrollmentDateTime").description("beginEnrollmentDateTime of event"),
                                fieldWithPath("closeEnrollmentDateTime").description("closeEnrollmentDateTime of event"),
                                fieldWithPath("beginEventDateTime").description("beginEventDateTime of event"),
                                fieldWithPath("endEventDateTime").description("endEventDateTime of event"),
                                fieldWithPath("location").description("location of event"),
                                fieldWithPath("basePrice").description("basePrice of event"),
                                fieldWithPath("maxPrice").description("maxPrice of event"),
                                fieldWithPath("limitOfEnrollment").description("limitOfEnrollment of event")
                        ),
                        responseHeaders(
                                headerWithName(HttpHeaders.LOCATION).description("location header"),
                                headerWithName(HttpHeaders.CONTENT_TYPE).description("content type header")
                        ),
                        relaxedResponseFields(
                                fieldWithPath("id").description("id of event"),
                                fieldWithPath("name").description("name of event"),
                                fieldWithPath("description").description("description of event"),
                                fieldWithPath("beginEnrollmentDateTime").description("beginEnrollmentDateTime of event"),
                                fieldWithPath("closeEnrollmentDateTime").description("closeEnrollmentDateTime of event"),
                                fieldWithPath("beginEventDateTime").description("beginEventDateTime of event"),
                                fieldWithPath("endEventDateTime").description("endEventDateTime of event"),
                                fieldWithPath("location").description("location of event"),
                                fieldWithPath("basePrice").description("basePrice of event"),
                                fieldWithPath("maxPrice").description("maxPrice of event"),
                                fieldWithPath("limitOfEnrollment").description("limitOfEnrollment of event"),
                                fieldWithPath("free").description("free of event"),
                                fieldWithPath("offline").description("offline of event"),
                                fieldWithPath("eventStatus").description("eventStatus of event"),
                                fieldWithPath("_links.self.href").description("link to self"),
                                fieldWithPath("_links.query-events.href").description("link to query events"),
                                fieldWithPath("_links.update-event.href").description("link to update event"),
                                fieldWithPath("_links.profile.href").description("link to profile")
                        )
                ))
                ;
    }

    private String getBearerToken() throws Exception {
        return "Bearer " + getAccessToken();
    }

    private String getAccessToken() throws Exception {
        // Given
        String username = appProperties.getUserUsername();
        String password = appProperties.getUserPassword();
        Account reddy = Account.builder()
                .email(username)
                .password(password)
                .roles(Set.of(AccountRole.ADMIN, AccountRole.USER))
                .build();
        this.accountService.saveAccount(reddy);

        // When
        String clientId = appProperties.getClientId();
        String clientSecret = appProperties.getClientSecret();
        ResultActions perform = this.mockMvc.perform(post("/oauth/token")
                .with(httpBasic(clientId, clientSecret))
                .param("username", username)
                .param("password", password)
                .param("grant_type", "password")
        );
        String responseBody = perform.andReturn().getResponse().getContentAsString();
        Jackson2JsonParser parser = new Jackson2JsonParser();
        return parser.parseMap(responseBody).get("access_token").toString();
    }

    @Test
    @DisplayName("입력받을 수 없는 값을 사용하면 에러가 발생하는 이벤트 생성 테스트")
    public void createEvent_badRequest() throws Exception {
        Event event = Event.builder()
                .id(100)
                .name("Spring")
                .description("REST API Development")
                .beginEnrollmentDateTime(LocalDateTime.of(2021,01,21,12,00,00))
                .closeEnrollmentDateTime(LocalDateTime.of(2021,01,22,12,00,00))
                .beginEventDateTime(LocalDateTime.of(2021,01,22,12,00,00))
                .endEventDateTime(LocalDateTime.of(2021,01,23,12,00,00))
                .basePrice(100)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("D2 스타트업 팩토리")
                .free(true)
                .offline(false)
                .eventStatus(EventStatus.PUBLISHED)
                .build();

        mockMvc.perform(post("/api/events")
                            .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaTypes.HAL_JSON)
                            .content(objectMapper.writeValueAsString(event))
                        )
                .andDo(print())
                .andExpect(status().isBadRequest()) // 400 응답으로 예상
        ;
    }

    @Test
    @DisplayName("입력값이 비어있는 경우 에러가 발생하는 이벤트 생성 테스트")
    public void createEvent_badRequest_emptyInput() throws Exception {
        EventDto eventDto = EventDto.builder().build();

        this.mockMvc.perform(post("/api/events")
                            .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(this.objectMapper.writeValueAsString(eventDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("입력 값이 잘못된 경우 에러가 발생하는 이벤트 생성 테스트")
    public void createEvent_badRequest_wrongInput() throws Exception {
        EventDto eventDto = EventDto.builder()
                .name("Spring")
                .description("REST API Development")
                .beginEnrollmentDateTime(LocalDateTime.of(2021,01,31,12,00,00))
                .closeEnrollmentDateTime(LocalDateTime.of(2021,01,30,12,00,00))
                .beginEventDateTime(LocalDateTime.of(2021,01,29,12,00,00))
                .endEventDateTime(LocalDateTime.of(2021,01,23,12,00,00))
                .basePrice(1000)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("D2 스타트업 팩토리")
                .build();

        this.mockMvc.perform(post("/api/events")
                    .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(eventDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("errors[0].objectName").exists())
                .andExpect(jsonPath("errors[0].defaultMessage").exists())
                .andExpect(jsonPath("errors[0].code").exists())
        ;
    }

    @Test
    @DisplayName("30개의 이벤트를 10개씩 묶어서 두번재 페이지 조회")
    public void queryEvents() throws Exception {
        // Given
        IntStream.range(0, 30).forEach(this::generateEvent);

        // When
        this.mockMvc.perform(get("/api/events")
                            .param("page", "1")
                            .param("size", "10")
                            .param("sort", "name,DESC"))
        // Then
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("page").exists())
                .andExpect(jsonPath("_embedded.eventList[0]._links.self").exists())
                .andExpect(jsonPath("_links.self").exists())
                .andExpect(jsonPath("_links.profile").exists())
                .andDo(document("query-events"))
        ;
    }

    @Test
    @DisplayName("이벤트 조회")
    public void getEvent() throws Exception {
        // Given
        Event event = this.generateEvent(100);

        // When
        this.mockMvc.perform(get("/api/events/{id}", event.getId()))
        // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").exists())
                .andExpect(jsonPath("id").exists())
                .andExpect(jsonPath("_links.self").exists())
                .andExpect(jsonPath("_links.profile").exists())
                .andDo(document("get-an-event"))
                ;
    }

    @Test
    @DisplayName("존재하지 않는 이벤트 조회시 404 응답 받기")
    public void getEvent404() throws Exception {
        // When
        this.mockMvc.perform(get("/api/events/9876432"))
        // Then
                .andExpect(status().isNotFound())
        ;
    }

    @Test
    @DisplayName("이벤트 정상적으로 수정")
    public void updateEvent() throws Exception {
        // Given
        Event event = this.generateEvent(200);
        EventDto eventDto = this.modelMapper.map(event, EventDto.class);
        String eventName = "updated name";
        eventDto.setName(eventName);

        // When
        this.mockMvc.perform(put("/api/events/{id}", event.getId())
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(eventDto))
        )
        // Then
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").exists())
                .andExpect(jsonPath("id").exists())
                .andExpect(jsonPath("_links.self").exists())
                .andExpect(jsonPath("_links.profile").exists())
                .andDo(document("update-event"))
        ;
    }

    @Test
    @DisplayName("입력값 비어있는 경우 이벤트 수정 실패")
    public void updateEvent400_Empty() throws Exception {
        // Given
        Event event = this.generateEvent(200);
        EventDto eventDto = new EventDto();

        // When
        this.mockMvc.perform(put("/api/events/{id}", event.getId())
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(eventDto))
        )
        // Then
                .andDo(print())
                .andExpect(status().isBadRequest())
        ;
    }

    @Test
    @DisplayName("입력값 잘못된 경우 이벤트 수정 실패")
    public void updateEvent400_WrongLogic() throws Exception {
        // Given
        Event event = this.generateEvent(200);
        EventDto eventDto = this.modelMapper.map(event, EventDto.class);
        eventDto.setBasePrice(50000);
        eventDto.setMaxPrice(100);


        // When
        this.mockMvc.perform(put("/api/events/{id}", event.getId())
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(eventDto))
        )
        // Then
                .andDo(print())
                .andExpect(status().isBadRequest())
        ;
    }

    @Test
    @DisplayName("존재하지 않는 이벤트 수정 실패")
    public void updateEvent404() throws Exception {
        // Given
        Event event = this.generateEvent(200);
        EventDto eventDto = this.modelMapper.map(event, EventDto.class);

        // When
        this.mockMvc.perform(put("/api/events/999324986", event.getId())
                .header(HttpHeaders.AUTHORIZATION, getBearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaTypes.HAL_JSON)
                .content(this.objectMapper.writeValueAsString(eventDto))
        )
       // Then
                .andDo(print())
                .andExpect(status().isNotFound())
        ;
    }

    private Event generateEvent(int index) {
        Event event = Event.builder()
                .name("Spring")
                .description("REST API Development")
                .beginEnrollmentDateTime(LocalDateTime.of(2021,01,21,12,00,00))
                .closeEnrollmentDateTime(LocalDateTime.of(2021,01,22,12,00,00))
                .beginEventDateTime(LocalDateTime.of(2021,01,22,12,00,00))
                .endEventDateTime(LocalDateTime.of(2021,01,23,12,00,00))
                .basePrice(100)
                .maxPrice(200)
                .limitOfEnrollment(100)
                .location("D2 스타트업 팩토리")
                .free(false)
                .offline(true)
                .eventStatus(EventStatus.DRAFT)
                .build();

        return this.eventRepository.save(event);
    }
}
