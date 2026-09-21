package rest.polymorphic;

import be.appify.prefab.core.spring.Page;
import be.appify.prefab.processor.rest.ControllerUtil;
import be.appify.prefab.test.TestUtil;
import java.lang.Exception;
import java.lang.String;
import java.util.List;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;
import rest.polymorphic.application.CreateShapeRequest;
import rest.polymorphic.infrastructure.http.ShapeResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ShapeClient {
    private final MockMvc mockMvc;

    private final JsonMapper jsonMapper;

    private final List<RequestPostProcessor> securityOverrides;

    public ShapeClient(WebApplicationContext context, JsonMapper jsonMapper,
            List<MockMvcConfigurer> configurers) {
        var builder = MockMvcBuilders.webAppContextSetup(context);
        AnnotationAwareOrderComparator.sort(configurers);
        configurers.forEach(builder::apply);
        this.mockMvc = builder.build();
        this.jsonMapper = jsonMapper;
        this.securityOverrides = List.of();
    }

    private ShapeClient(MockMvc mockMvc, JsonMapper jsonMapper,
            List<RequestPostProcessor> securityOverrides) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
        this.securityOverrides = securityOverrides;
    }

    public ShapeClient as(RequestPostProcessor... requestPostProcessors) {
        return new ShapeClient(mockMvc, jsonMapper, List.of(requestPostProcessors));
    }

    private RequestPostProcessor applySecurityOverride(RequestPostProcessor defaultPostProcessor) {
        if (securityOverrides.isEmpty()) {
            return defaultPostProcessor;
        }
        return request -> {
                    for (RequestPostProcessor override : securityOverrides) {
                        request = override.postProcessRequest(request);
                    }
                    return request;
                };
    }

    public String create(CreateShapeRequest request) throws Exception {
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/shapes")
                .with(applySecurityOverride(SecurityMockMvcRequestPostProcessors.user("test")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
        return TestUtil.idOf(result);
    }

    public String createCircle(double radius) throws Exception {
        return create(new CreateShapeRequest.CreateCircleRequest(radius));
    }

    public String createRectangle(double width, double height) throws Exception {
        return create(new CreateShapeRequest.CreateRectangleRequest(width, height));
    }

    public ShapeResponse getShapeById(String id) throws Exception {
        var json = mockMvc.perform(MockMvcRequestBuilders.get("/shapes/{id}", id)
                .with(applySecurityOverride(SecurityMockMvcRequestPostProcessors.user("test")))
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return jsonMapper.readValue(json, ShapeResponse.class);
    }

    public void deleteShape(String id) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/shapes/{id}", id)
                .with(applySecurityOverride(SecurityMockMvcRequestPostProcessors.user("test"))))
                        .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    public void whenDeletingShape(String id) throws Exception {
        deleteShape(id);
    }

    public void givenShapeDeleted(String id) throws Exception {
        deleteShape(id);
    }

    public Page<ShapeResponse> findShapes(Pageable pageable) throws Exception {
        var request = MockMvcRequestBuilders.get("/shapes")
                .with(applySecurityOverride(SecurityMockMvcRequestPostProcessors.user("test")));
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
