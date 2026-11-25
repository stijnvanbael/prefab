package com.example.school;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Student(
        @Id Reference<Student> id,
        @Version long version,
        @NotBlank String name,
        @NotBlank @Email String email,
        int activeEnrollments
) {
    @Create
    public Student(@NotBlank String name, @NotBlank @Email String email) {
        this(Reference.create(), 0L, name, email, 0);
    }

    @EventHandler
    @ByReference(property = "student")
    public Student onEnrolled(EnrollmentEvent.Enrolled event) {
        return new Student(id, version, name, email, activeEnrollments + 1);
    }

    @EventHandler
    @ByReference(property = "student")
    public Student onWithdrawn(EnrollmentEvent.Withdrawn event) {
        return new Student(id, version, name, email, activeEnrollments - 1);
    }

    @EventHandler
    @ByReference(property = "student")
    public Student onCancelled(EnrollmentEvent.Cancelled event) {
        return new Student(id, version, name, email, activeEnrollments - 1);
    }
}
