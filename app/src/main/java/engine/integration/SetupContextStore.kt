package engine.integration

import engine.setup.Setup

interface SetupContextStore {

    fun put(
        decisionId: String,
        setup: Setup,
        expiresAt: Long
    )

    fun get(
        decisionId: String
    ): Setup?

    fun remove(
        decisionId: String
    )
}
