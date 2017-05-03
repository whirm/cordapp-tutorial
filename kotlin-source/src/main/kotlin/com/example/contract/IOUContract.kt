package com.example.contract

import com.example.state.IOUState
import net.corda.core.contracts.*
import net.corda.core.contracts.clauses.*
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
open class IOUContract : Contract {
    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: TransactionForContract) =
            verifyClause(tx, AnyOf(Clauses.Group()), tx.commands.select<Commands>())

    interface Clauses {
        class Group : GroupClauseVerifier<IOUState, Commands, UniqueIdentifier>(FirstOf(Create(), Spend())) {
            override fun groupStates(tx: TransactionForContract): List<TransactionForContract.InOutGroup<IOUState, UniqueIdentifier>>
                    = tx.groupStates(IOUState::linearId)
        }

        class Create : Clause<IOUState, Commands, UniqueIdentifier>() {
            override val requiredCommands: Set<Class<out CommandData>> = setOf(Commands.Create::class.java)

            override fun verify(tx: TransactionForContract, inputs: List<IOUState>, outputs: List<IOUState>, commands: List<AuthenticatedObject<Commands>>, groupingKey: UniqueIdentifier?): Set<Commands> {
                val command = tx.commands.requireSingleCommand<Commands.Create>()
                requireThat {
                    // Generic constraints around the IOU transaction.
                    "No inputs should be consumed when issuing an IOU." by (tx.inputs.isEmpty())
                    "Only one output state should be created." by (tx.outputs.size == 1)
                    val out = tx.outputs.single() as IOUState
                    "The sender and the recipient cannot be the same entity." by (out.sender != out.recipient)
                    "All of the participants must be signers." by (command.signers.containsAll(out.participants))

                    // IOU-specific constraints.
                    "The IOU's value must be non-negative." by (out.iou.value > 0)
                }
                return setOf(command.value)
            }
        }

        class Spend : Clause<IOUState, Commands, UniqueIdentifier>() {
            override val requiredCommands: Set<Class<out CommandData>> = setOf(Commands.Spend::class.java)

            override fun verify(tx: TransactionForContract,
                                inputs: List<IOUState>,
                                outputs: List<IOUState>,
                                commands: List<AuthenticatedObject<Commands>>,
                                groupingKey: UniqueIdentifier?): Set<Commands> {
                val command = tx.commands.requireSingleCommand<Commands.Spend>()
                requireThat {
                    "Only one input should be consumed when spending a state." by (inputs.size == 1)
                    "No output state should be created." by (outputs.isEmpty())
                }
                return setOf(command.value)
            }
        }
    }


    interface Commands : CommandData {
        class Create : Commands
        class Spend : Commands
    }

    /** This is a reference to the underlying legal contract template and associated parameters. */
    override val legalContractReference: SecureHash = SecureHash.sha256("IOU contract template and params")
}
