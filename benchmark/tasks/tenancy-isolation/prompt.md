The application becomes multi-tenant. Every request carries the tenant in the HTTP header `X-Tenant-Id` (the API
gateway guarantees it is present and trustworthy).

Make projects (`com.example.planning.Project`) tenant-specific:

- a project belongs to the tenant of the request that created it; clients never send the tenant in the body;
- `GET /projects/{id}` and `GET /projects` only ever return projects of the requesting tenant (a project of another
  tenant is `404 Not Found`, and is absent from the list).
