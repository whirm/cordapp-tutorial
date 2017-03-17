package com.example.contract

import com.example.state.IOUState
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [IOUState], which in turn encapsulates an [IOU].
 *
 * For a new [IOU] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [IOU].
 * - An Create() command with the public keys of both the sender and the recipient.
 *
 * All contracts must sub-class the [Contract] interface.
 */
class TokenContract : Contract {

    /** The verify() function must not throw an exception if, and only if, the transaction is valid. */
    override fun verify(tx: TransactionForContract) {
        val command = tx.commands.requireSingleCommand<Issue>()
        requireThat {
            "No inputs should be consumed when issuing an IOU." by (tx.inputs.isEmpty())
            "Only one output state should be created." by (tx.outputs.size == 1)
            val participants = tx.outputs.single().participants
            "All of the participants must be signers." by (command.signers.containsAll(participants))
        }
    }

    /** This contract only implements one command, Issue. */
    class Issue : CommandData

    /** This is a reference to the underlying legal contract template and associated parameters. */
    override val legalContractReference: SecureHash = SecureHash.sha256("IOU contract template and params")
}
