package be.appify.prefab.example.mongodb.shape;

import be.appify.prefab.example.mongodb.shape.application.ShapeRepository;
import be.appify.prefab.example.mongodb.shape.infrastructure.http.ShapeResponse;
import be.appify.prefab.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ShapeIntegrationTest {

    @Autowired
    ShapeRepository shapeRepository;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    void saveCircleAndRetrieveAsShape() throws Exception {
        var circle = new Shape.Circle(7.5);
        shapeRepository.save(circle);

        var json = mockMvc.perform(get("/shapes/{id}", circle.id().id())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var response = jsonMapper.readValue(json, ShapeResponse.class);

        assertThat(response).isInstanceOf(ShapeResponse.CircleResponse.class);
        var circleResponse = (ShapeResponse.CircleResponse) response;
        assertThat(circleResponse.radius()).isEqualTo(7.5);
        assertThat(circleResponse.id().id()).isEqualTo(circle.id().id());
    }

    @Test
    void saveRectangleAndRetrieveAsShape() throws Exception {
        var rectangle = new Shape.Rectangle(4.0, 3.0);
        shapeRepository.save(rectangle);

        var json = mockMvc.perform(get("/shapes/{id}", rectangle.id().id())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var response = jsonMapper.readValue(json, ShapeResponse.class);

        assertThat(response).isInstanceOf(ShapeResponse.RectangleResponse.class);
        var rectangleResponse = (ShapeResponse.RectangleResponse) response;
        assertThat(rectangleResponse.width()).isEqualTo(4.0);
        assertThat(rectangleResponse.height()).isEqualTo(3.0);
        assertThat(rectangleResponse.id().id()).isEqualTo(rectangle.id().id());
    }

    @Test
    void pageThroughMixedListOfShapes() throws Exception {
        var circle = new Shape.Circle(1.0);
        var rectangle = new Shape.Rectangle(2.0, 3.0);
        shapeRepository.save(circle);
        shapeRepository.save(rectangle);

        var json = mockMvc.perform(get("/shapes")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        // Verify that each shape's specific fields are present in the response.
        // One element should be a Circle (has 'radius'), the other a Rectangle (has 'width' and 'height').
        assertThat(json).contains("\"radius\":1.0");
        assertThat(json).contains("\"width\":2.0");
        assertThat(json).contains("\"height\":3.0");
    }

    @Test
    void retrieveUnknownShapeReturns404() throws Exception {
        mockMvc.perform(get("/shapes/nonexistent-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
