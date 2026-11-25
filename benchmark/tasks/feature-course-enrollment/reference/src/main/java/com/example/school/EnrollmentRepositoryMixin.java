package com.example.school;

import be.appify.prefab.core.annotations.RepositoryMixin;
import be.appify.prefab.core.service.Reference;
import java.util.List;

@RepositoryMixin(Enrollment.class)
public interface EnrollmentRepositoryMixin {
    List<Enrollment> findByCourse(Reference<Course> course);
}
