package be.appify.prefab.spike.usecase.importing.http;

import be.appify.prefab.spike.usecase.importing.ProductImportWorkflow;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class ProductImportWorkflowEndpoint {
    private final be.appify.prefab.spike.usecase.importing.ProductImportWorkflow workflow;

    public ProductImportWorkflowEndpoint(be.appify.prefab.spike.usecase.importing.ProductImportWorkflow workflow) {
        this.workflow = workflow;
    }

    @RequestMapping(path = "/products/import", method = RequestMethod.POST)
    public be.appify.prefab.spike.usecase.importing.ProductImportResult invoke(
            @Valid @RequestBody be.appify.prefab.spike.usecase.importing.ProductImportRequest request) {
        return workflow.execute(request);
    }
}
