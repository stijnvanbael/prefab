package delete.cascade.application;

import delete.cascade.Organisation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrganisationService {
    private static final Logger log = LoggerFactory.getLogger(OrganisationService.class);

    private final OrganisationRepository organisationRepository;

    private final DepartmentRepository departmentRepository;

    private final EmployeeRepository employeeRepository;

    public OrganisationService(OrganisationRepository organisationRepository,
            DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.organisationRepository = organisationRepository;
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public void delete(String id) {
        log.debug("Deleting {} with id: {}", Organisation.class.getSimpleName(), id);
        organisationRepository.deleteById(id);
    }
}
    