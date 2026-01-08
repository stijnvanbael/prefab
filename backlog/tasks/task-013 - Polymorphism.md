---
id: task-013
title: Polymorphism
status: To Do
assignee: []
created_date: '2025-10-10 13:34'
updated_date: '2026-01-08 09:09'
labels: []
dependencies: []
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Currently not supported by Spring Data JDBC

Possible solution: generate converters

```
@ReadingConverter
public class AssessmentReadingConverter implements Converter<ResultSet, Assessment> {
    @Override
    public Assessment convert(ResultSet rs) {
        String type = rs.getString("type");
        // Example: switch on type to instantiate the correct subtype
        if ("MULTIPLE_CHOICE".equals(type)) {
            // Map fields to MultipleChoiceAssessment
            // (extract fields from rs and construct the object)
            return new MultipleChoiceAssessment(/* ... */);
        }
        // Add more subtypes as needed
        throw new IllegalArgumentException("Unknown assessment type: " + type);
    }
}
```
<!-- SECTION:DESCRIPTION:END -->
