package co.wordbe.eventrestapi.events;

import co.wordbe.eventrestapi.accounts.Account;
import co.wordbe.eventrestapi.accounts.AccountAdapter;
import co.wordbe.eventrestapi.accounts.CurrentUser;
import co.wordbe.eventrestapi.common.ErrorsResource;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
@RequestMapping(value = "/api/events", produces = MediaTypes.HAL_JSON_VALUE)
public class EventController {

    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;
    private final EventValidator eventValidator;

    public EventController(EventRepository eventRepository, ModelMapper modelMapper, EventValidator eventValidator) {
        this.eventRepository = eventRepository;
        this.modelMapper = modelMapper;
        this.eventValidator = eventValidator;
    }

    @PostMapping
    public ResponseEntity createEvent(@RequestBody @Valid EventDto eventDto, Errors errors,
                                      @CurrentUser Account currentUser) {
        // 1. EventDto 필드 조건 에러
        if (errors.hasErrors()) return badReqeust(errors);

        // 2. EventDto 비즈니스 로직 에러
        eventValidator.validate(eventDto, errors);
        if (errors.hasErrors()) return badReqeust(errors);

        Event event = modelMapper.map(eventDto, Event.class);
        event.update();
        event.setManager(currentUser);

        Event newEvent = this.eventRepository.save(event);

        WebMvcLinkBuilder selfLinkBuilder = linkTo(EventController.class).slash(newEvent.getId());
        URI createdUri = selfLinkBuilder.toUri();

//        EventResource eventResource = new EventResource(event);
        EntityModel<Event> eventResource = EventResource.modelOf(event);
        eventResource.add(selfLinkBuilder.withRel("update-event"));
        eventResource.add(linkTo(EventController.class).withRel("query-events"));
        eventResource.add(Link.of("/docs/index.html#resources-events-create").withRel("profile"));

        return ResponseEntity.created(createdUri).body(eventResource);
    }

    @GetMapping
    public ResponseEntity queryEvents(Pageable pageable,
                                      PagedResourcesAssembler<Event> assembler,
                                      @CurrentUser Account account) {

        Page<Event> page = this.eventRepository.findAll(pageable);
        var pagedResources = assembler.toModel(page, e -> EventResource.modelOf(e));
        pagedResources.add(Link.of("/docs/index.html#resources-events-list").withRel("profile"));

        if (account != null) {
            pagedResources.add(linkTo(EventController.class).withRel("create-event"));
        }

        return ResponseEntity.ok(pagedResources);
    }

    @GetMapping("/{id}")
    public ResponseEntity getEent(@PathVariable Integer id,
                                  @CurrentUser Account currentUser) {
        Optional<Event> optionalEvent = this.eventRepository.findById(id);
        if (optionalEvent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Event event = optionalEvent.get();
        EntityModel<Event> eventResource = EventResource.modelOf(event);
        eventResource.add(Link.of("/docs/index.html#resources-events-get").withRel("profile"));

        if (event.getManager().equals(currentUser)) {
            eventResource.add(linkTo(EventController.class).slash(event.getId()).withRel("update-event"));
        }

        return ResponseEntity.ok(eventResource);
    }

    @PutMapping("/{id}")
    public ResponseEntity updateEvent(@PathVariable Integer id,
                                      @RequestBody @Valid EventDto eventDto,
                                      Errors errors,
                                      @CurrentUser Account currentUser) {
        Optional<Event> optionalEvent = this.eventRepository.findById(id);

        // 404 Not Found
        if (optionalEvent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // 400 NotEmpty, NonNull, Min etc (DTO validation)
        if (errors.hasErrors()) {
            return badReqeust(errors);
        }

        // 400 Wrong business Logic
        this.eventValidator.validate(eventDto, errors);
        if (errors.hasErrors()) {
            return badReqeust(errors);
        }

        Event event = optionalEvent.get();
        if (!event.getManager().equals(currentUser)) {
            return new ResponseEntity(HttpStatus.UNAUTHORIZED);
        }

        // eventDto 에서 event 로
        this.modelMapper.map(eventDto, event);
        Event saveEvent = this.eventRepository.save(event);

        EntityModel<Event> eventResource = EventResource.modelOf(saveEvent);
        eventResource.add(Link.of("/docs/index.html#resources-events-update").withRel("profile"));
        return ResponseEntity.ok(eventResource);
    }

    private ResponseEntity badReqeust(Errors errors) {
        return ResponseEntity.badRequest().body(ErrorsResource.modelOf(errors));
    }
}
