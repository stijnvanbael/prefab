We are building course registration for an evening school as a Spring Boot application with Prefab. The project is
empty; put the code in package `com.example.school`.

**User story.** As a student, I want to enroll in courses and withdraw from them; as the school, I want to cancel a
course and have all its students taken off it automatically.

Acceptance criteria:

1. `POST /courses` with `{"title": "Pottery", "capacity": 12}` adds a course (`201 Created`, `Location` header ending
   in the id). `GET /courses/{id}` returns `id`, `title`, `capacity`, `enrolled` (the number of students currently
   enrolled, starting at 0) and `status` (`OPEN`).
2. `POST /students` with `{"name": "Ada", "email": "ada@example.com"}` registers a student. `GET /students/{id}`
   returns `id`, `name`, `email` and `activeEnrollments` (starting at 0).
3. `POST /enrollments` with `{"course": "<course id>", "student": "<student id>"}` enrolls a student (`201 Created`).
   `GET /enrollments/{id}` returns `id`, `course`, `student` and `status` (`ENROLLED`). The course's `enrolled` and
   the student's `activeEnrollments` go up by one.
4. `POST /enrollments/{id}/withdrawal` (`200 OK`) withdraws: the enrollment becomes `WITHDRAWN`, and both counters go
   down by one. Withdrawing twice changes nothing.
5. `POST /courses/{id}/cancellation` (`200 OK`) cancels a course: its status becomes `CANCELLED`, every enrollment of
   that course that is still `ENROLLED` becomes `CANCELLED`, and those students' `activeEnrollments` go down by one.
   Withdrawn enrollments stay `WITHDRAWN`. Cancelling a course without enrollments simply works.
6. `GET /enrollments?course=<course id>` lists the enrollments of one course, as a page (in the `content` array).

Counters and cancellations that follow from another change may lag a moment behind.
