package be.appify.prefab.streams.mongodb;

import be.appify.prefab.core.diagnostics.FailureReport;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Explains why the stream state repository refuses to start on a standalone MongoDB server. */
class MongoTransactionsFailureAnalyzer extends AbstractFailureAnalyzer<MongoTransactionsUnavailableException> {
    private static final FailureReport REPORT = new FailureReport(
            "prefab-streams-mongodb needs MongoDB transactions, but the server in spring.mongodb.uri is a "
                    + "standalone instance.",
            "Run MongoDB as a replica set (a single-node replica set is enough, for example "
                    + "'mongod --replSet rs0' followed by rs.initiate()) or as a sharded cluster, and connect with "
                    + "spring.mongodb.uri=mongodb://host:27017/<database>?replicaSet=rs0.");

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MongoTransactionsUnavailableException cause) {
        return REPORT.analysisOf(rootFailure);
    }
}
