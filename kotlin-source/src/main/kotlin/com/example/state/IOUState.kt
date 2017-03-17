package com.example.state

import com.example.contract.TokenContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.CompositeKey
import net.corda.core.crypto.Party
import java.security.PublicKey

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param issuer the party issuing the IOU.
 * @param contract the contract which governs which transactions are valid for this state object.
 */
data class IOUState(val issuer: Party,
                    override val contract: TokenContract,
                    override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState {

    /** The public keys of the involved parties. */
    override val participants: List<CompositeKey> get() = listOf(issuer.owningKey)

    /** Tells the vault to track a state if we are one of the parties involved. */
    override fun isRelevant(ourKeys: Set<PublicKey>) = ourKeys.intersect(participants.flatMap {it.keys}).isNotEmpty()
}
