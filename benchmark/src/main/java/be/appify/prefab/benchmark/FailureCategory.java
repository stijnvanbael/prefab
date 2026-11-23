package be.appify.prefab.benchmark;

/**
 * Why a task failed. Each category maps to a kind of follow-up work on Prefab or on the benchmark itself.
 */
enum FailureCategory {
    /** The model did not know how to do it with Prefab: add or improve documentation, cheat sheet or recipe. */
    DOC_GAP,
    /** A Prefab compiler or runtime message sent the model the wrong way or did not help: improve the diagnostic. */
    MISLEADING_DIAGNOSTIC,
    /** The model used a Prefab API in a way it does not support or named things differently than asked. */
    API_MISUSE,
    /** The model wrote plain Spring/JPA code instead of letting Prefab generate it. */
    PLAIN_SPRING_FALLBACK,
    /** The benchmark task, its tests or the environment (Docker, network, agent API) failed, not the model. */
    TEST_OR_INFRASTRUCTURE
}
