package co.wordbe.eventrestapi.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder @AllArgsConstructor @NoArgsConstructor
@Data
public class EventDto {

    private String name;
    private String description;

    private LocalDateTime beginEnrollmentDateTime;
    private LocalDateTime closeEnrollmentDateTime;
    private LocalDateTime beginEventDateTime;
    private LocalDateTime endEventDateTime;

    private String location; // null 이면 온라인 모임
    private int basePrice;
    private int maxPrice;
    private int limitOfEnrollment;
}
