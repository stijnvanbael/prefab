We are building expense claims for our company as a Spring Boot application with Prefab. The project is empty; put
the code in package `com.example.expenses`. The API is secured with OAuth2 bearer tokens (already configured); roles
are standard Spring Security roles (authority `ROLE_EMPLOYEE`, `ROLE_MANAGER`, `ROLE_FINANCE`).

**User story.** As an employee, I want to claim back what I paid for the company; as a manager, I want to approve or
reject claims; as finance, I want to pay approved claims, so that employees get their money back.

Acceptance criteria:

1. Employees (role `EMPLOYEE`) submit a claim with `POST /expense-claims` and
   `{"description": "Train to Brussels", "amount": 42.50}` (`201 Created`, `Location` header ending in the id). The
   amount must be positive (`400 Bad Request` otherwise). Users without the role get `403 Forbidden`.
2. `GET /expense-claims/{id}` (any signed-in user) returns `id`, `description`, `amount`, `status` (`SUBMITTED`) and
   `submittedBy`: the name of the signed-in user who submitted it. Clients cannot set `submittedBy`.
3. Managers (role `MANAGER`) approve a submitted claim with `POST /expense-claims/{id}/approval` or reject it with
   `POST /expense-claims/{id}/rejection` (`200 OK`; status `APPROVED` or `REJECTED`). Other users get
   `403 Forbidden`. A claim that is no longer `SUBMITTED` does not change.
4. Approving a claim creates a reimbursement for it automatically, exactly one per claim.
   `GET /reimbursements?claim=<claim id>` returns them as a page (reimbursements in the `content` array) and
   `GET /reimbursements/{id}` returns one: `id`, `claim`, `employee` (who submitted the claim), `amount` and `status`
   (`PENDING`).
5. Finance (role `FINANCE`) pays a reimbursement with `POST /reimbursements/{id}/payment` (`200 OK`, status `PAID`);
   the claim then becomes `REIMBURSED`. Only finance may read or pay reimbursements; others get `403 Forbidden`.

Steps that follow from another step (a reimbursement after an approval, a reimbursed claim after a payment) may lag a
moment behind.
