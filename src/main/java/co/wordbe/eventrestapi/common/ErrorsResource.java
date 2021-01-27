package co.wordbe.eventrestapi.common;

import co.wordbe.eventrestapi.index.IndexController;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.validation.Errors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public class ErrorsResource extends EntityModel<Errors> {
//    public ErrorsResource(Errors content, Link... links) {
//        super(content, links);
//        add(linkTo(methodOn(IndexController.class).index()).withRel("index"));
//    }
    public static EntityModel<Errors> modelOf(Errors errors) {
        EntityModel<Errors> errorsEntityModel = EntityModel.of(errors);
        errorsEntityModel.add(linkTo(methodOn(IndexController.class).index()).withRel("index"));
        return errorsEntityModel;
    }
}
