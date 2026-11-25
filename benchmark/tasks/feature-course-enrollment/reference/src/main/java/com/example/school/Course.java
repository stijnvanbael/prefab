package com.example.school;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Course(
        @Id Reference<Course> id,
        @Version long version,
        @NotBlank String title,
        @Positive int capacity,
        int enrolled,
        CourseStatus status
) {
    private static final EventPublisher<CourseCancelled> CANCELLED = EventPublisher.bind(CourseCancelled.class);

    @Create
    public Course(@NotBlank String title, @Positive int capacity) {
        this(Reference.create(), 0L, title, capacity, 0, CourseStatus.OPEN);
    }

    @Update(path = "/cancellation", method = "POST")
    public Course cancel() {
        if (status == CourseStatus.CANCELLED) {
            return this;
        }
        CANCELLED.publish(new CourseCancelled(id));
        return new Course(id, version, title, capacity, enrolled, CourseStatus.CANCELLED);
    }

    @EventHandler
    @ByReference(property = "course")
    public Course onEnrolled(EnrollmentEvent.Enrolled event) {
        return new Course(id, version, title, capacity, enrolled + 1, status);
    }

    @EventHandler
    @ByReference(property = "course")
    public Course onWithdrawn(EnrollmentEvent.Withdrawn event) {
        return new Course(id, version, title, capacity, enrolled - 1, status);
    }

    @EventHandler
    @ByReference(property = "course")
    public Course onCancelled(EnrollmentEvent.Cancelled event) {
        return new Course(id, version, title, capacity, enrolled - 1, status);
    }
}
