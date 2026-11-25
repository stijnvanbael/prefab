package com.example.school;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "enrollments")
public sealed interface EnrollmentEvent
        permits EnrollmentEvent.Enrolled, EnrollmentEvent.Withdrawn, EnrollmentEvent.Cancelled {
    @PartitioningKey
    Reference<Enrollment> enrollment();

    Reference<Course> course();

    Reference<Student> student();

    record Enrolled(Reference<Enrollment> enrollment, Reference<Course> course, Reference<Student> student)
            implements EnrollmentEvent {
    }

    record Withdrawn(Reference<Enrollment> enrollment, Reference<Course> course, Reference<Student> student)
            implements EnrollmentEvent {
    }

    record Cancelled(Reference<Enrollment> enrollment, Reference<Course> course, Reference<Student> student)
            implements EnrollmentEvent {
    }
}
