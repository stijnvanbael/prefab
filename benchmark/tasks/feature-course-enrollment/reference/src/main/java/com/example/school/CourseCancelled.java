package com.example.school;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "courses")
public record CourseCancelled(@PartitioningKey Reference<Course> course) {
}
