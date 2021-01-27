package co.wordbe.eventrestapi.events;

import co.wordbe.eventrestapi.index.IndexController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.validation.Errors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class EventResource extends EntityModel<Event> {
//    public EventResource(Event event, Link... links) {
//        super(event, links);
//        add(linkTo(EventController.class).slash(event.getId()).withSelfRel());
//    }
    public static EntityModel<Event> modelOf(Event event) {
        EntityModel<Event> eventEntityModel = EntityModel.of(event);
        eventEntityModel.add(linkTo(EventController.class).slash(event.getId()).withSelfRel());
        return eventEntityModel;
    }
}
