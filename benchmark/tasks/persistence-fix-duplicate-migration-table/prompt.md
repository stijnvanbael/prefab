The released V1 migration already creates the `product` table, but V2 tries to create it again with an obsolete
column list. Compilation now rejects that conflicting schema transition.

Make V2 a safe no-op when `product` already exists by using PostgreSQL's existence guard. Do not edit V1 or the
`Product` aggregate, and do not replace the released table definition. The build should succeed without generating
another migration.
