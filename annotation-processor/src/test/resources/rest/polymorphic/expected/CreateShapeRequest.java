package rest.polymorphic.application;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
public sealed interface CreateShapeRequest permits CreateShapeRequest.CreateCircleRequest, CreateShapeRequest.CreateRectangleRequest {
    record CreateCircleRequest(double radius) implements CreateShapeRequest {
    }

    record CreateRectangleRequest(double width, double height) implements CreateShapeRequest {
    }
}
