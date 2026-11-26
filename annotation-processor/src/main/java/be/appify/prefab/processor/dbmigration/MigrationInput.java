package be.appify.prefab.processor.dbmigration;

import java.util.List;

/** The desired schema of one aggregate, before diffing against the module's Flyway migrations. */
record MigrationInput(String name, List<Table> tables, boolean trigrams) {}
