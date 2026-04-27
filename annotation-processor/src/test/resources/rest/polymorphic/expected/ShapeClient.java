package rest.polymorphic;

import be.appify.prefab.core.spring.Page;
import be.appify.prefab.processor.rest.ControllerUtil;
import be.appify.prefab.test.TestUtil;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import rest.polymorphic.application.CreateCircleRequest;
import rest.polymorphic.application.CreateRectangleRequest;
import rest.polymorphic.infrastructure.http.ShapeResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ShapeClient {
    private final MockMvc mockMvc;

    private final JsonMapper jsonMapper;

    public ShapeClient(WebApplicationContext context, JsonMapper jsonMapper) {
        this.mockMvc = MockMvcBuilders
                    .webAppContextSetup(context)
                    .build();
        this.jsonMapper = jsonMapper;
    }

    public String createCircle(double radius) throws Exception {
        return createCircle(new CreateCircleRequest(radius));
    }

    public String createCircle(CreateCircleRequest circle) throws Exception {
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/shapes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(circle)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
        return TestUtil.idOf(result);
    }

    public String createRectangle(double width, double height) throws Exception {
        return createRectangle(new CreateRectangleRequest(width, height));
    }

    public String createRectangle(CreateRectangleRequest rectangle) throws Exception {
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/shapes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(rectangle)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
        return TestUtil.idOf(result);
    }

    public ShapeResponse getShapeById(String id) throws Exception {
        var json = mockMvc.perform(MockMvcRequestBuilders.get("/shapes/{id}", id)
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return jsonMapper.readValue(json, ShapeResponse.class);
    }

    public void deleteShape(String id) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/shapes/{id}", id))
                        .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    public void whenDeletingShape(String id) throws Exception {
        deleteShape(id);
    }

    public void givenShapeDeleted(String id) throws Exception {
        deleteShape(id);
    }

    public Page<ShapeResponse> findShapes(Pageable pageable) throws Exception {
        var request = MockMvcRequestBuilders.get("/shapes");
        if (pageable != null && pageable.isPaged()) {
            request.queryParam("page", String.valueOf(pageable.getPageNumber()))
                   .queryParam("size", String.valueOf(pageable.getPageSize()));
        }
        if (pageable != null && pageable.getSort().isSorted()) {
            request.queryParam("sort", ControllerUtil.toRequestParams(pageable.getSort()));
        }
        var json = mockMvc.perform(request.accept(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return jsonMapper.readValue(json, new TypeReference<Page<ShapeResponse>>() {});
    }
}

