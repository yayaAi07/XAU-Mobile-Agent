package engine.integration

import engine.setup.Setup
import java.util.concurrent.ConcurrentHashMap

class InMemorySetupContextStore : SetupContextStore {

    private data class Entry(
        val setup: Setup,
        val expiresAt: Long
    )

    private val store =
        ConcurrentHashMap<String, Entry>()

    override fun put(
        decisionId: String,
        setup: Setup,
        expiresAt: Long
    ) {
        sweepExpired()

        store[decisionId] =
            Entry(
                setup = setup,
                expiresAt = expiresAt
            )
    }

    override fun get(
        decisionId: String
    ): Setup? {

        val entry =
            store[decisionId]
                ?: return null

        val now =
            System.currentTimeMillis()

        if (now >= entry.expiresAt) {
            store.remove(
                decisionId,
                entry
            )

            return null
        }

        return entry.setup
    }

    override fun remove(
        decisionId: String
    ) {
        store.remove(decisionId)
    }

    private fun sweepExpired() {

        val now =
            System.currentTimeMillis()

        store.forEach { (decisionId, entry) ->

            if (now >= entry.expiresAt) {
                store.remove(
                    decisionId,
                    entry
                )
            }
        }
    }
}
