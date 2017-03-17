package com.example.flow

import co.paralleluniverse.fibers.Suspendable
import com.example.contract.TokenContract
import com.example.state.IOUState
import net.corda.core.contracts.Command
import net.corda.core.contracts.TransactionType
import net.corda.core.flows.FlowLogic
import net.corda.flows.FinalityFlow

/** This flow allows a party to issue a token onto the ledger. */
class IssuerFlow(val iou: IOUState) : FlowLogic<Unit>() {

    /** The flow logic is encapsulated within the call() method. */
    @Suspendable override fun call() {
        // Stage 1 - Building the transaction.
        val txCommand = Command(TokenContract.Issue(), iou.participants)
        val notary = serviceHub.networkMapCache.notaryNodes.single().notaryIdentity
        val txBuilder = TransactionType.General.Builder(notary).withItems(iou, txCommand)

        // Stage 2 - Verifying the transaction.
        txBuilder.toWireTransaction().toLedgerTransaction(serviceHub).verify()

        // Stage 3 - Signing the transaction.
        val signedTx = txBuilder.signWith(serviceHub.legalIdentityKey).toSignedTransaction(checkSufficientSignatures = false)

        // Stage 4 - Notarising and recording the transaction.
        subFlow(FinalityFlow(signedTx, setOf(serviceHub.myInfo.legalIdentity)))
    }
}