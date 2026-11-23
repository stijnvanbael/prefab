Contracts (`com.example.legal.Contract`) need an audit trail. `GET /contracts/{id}` must return an `audit` object:

```json
{"id": "...", "title": "...", "audit": {"createdBy": "...", "createdAt": "...", "lastModifiedBy": "...", "lastModifiedAt": "..."}}
```

- `createdBy`/`createdAt` are set when the contract is created and never change;
- `lastModifiedBy`/`lastModifiedAt` are set on creation and on every update (`PUT /contracts/{id}`);
- the user is the name of the authenticated user (Spring Security principal name); times are timestamps.

Clients cannot set these values themselves.
