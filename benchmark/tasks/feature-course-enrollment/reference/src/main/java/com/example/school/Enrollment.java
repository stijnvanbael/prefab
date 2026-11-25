package com.example.school;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Enrollment(
        @Id Reference<Enrollment> id,
        @Version long version,
        @NotNull @Filter Reference<Course> course,
        @NotNull Reference<Student> student,
        EnrollmentStatus status
) {
    private static final EventPublisher<EnrollmentEvent> EVENTS = EventPublisher.bind(EnrollmentEvent.class);

    @Create
    public Enrollment(@NotNull Reference<Course> course, @NotNull Reference<Student> student) {
        this(Reference.create(), 0L, course, student, EnrollmentStatus.ENROLLED);
        EVENTS.publish(new EnrollmentEvent.Enrolled(id, course, student));
    }

    @Update(path = "/withdrawal", method = "POST")
    public Enrollment withdraw() {
        if (status != EnrollmentStatus.ENROLLED) {
            return this;
        }
        EVENTS.publish(new EnrollmentEvent.Withdrawn(id, course, student));
        return new Enrollment(id, version, course, student, EnrollmentStatus.WITHDRAWN);
    }

    @EventHandler
    @Multicast(queryMethod = "findByCourse", parameters = "course", failWhenEmpty = false)
    public Enrollment onCourseCancelled(CourseCancelled event) {
        if (status != EnrollmentStatus.ENROLLED) {
            return this;
        }
        EVENTS.publish(new EnrollmentEvent.Cancelled(id, course, student));
        return new Enrollment(id, version, course, student, EnrollmentStatus.CANCELLED);
    }
}
