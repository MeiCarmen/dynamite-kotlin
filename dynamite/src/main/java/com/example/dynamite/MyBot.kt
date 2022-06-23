package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move

data class Likelyhood(var move: Move, var prob: Int)

class MyBot : Bot {
    private var ourDynamitesPlayed = 0
    private var theirDynamitesPlayed = 0
    private var moves = arrayListOf(Move.R, Move.P, Move.S, Move.D, Move.W)
    private var moveBasedOnMyPrev = mutableMapOf<Move, MutableMap<Move, Int>>()
    private var moveBasedOnTheirPrev = mutableMapOf<Move, MutableMap<Move, Int>>()
    override fun makeMove(gamestate: Gamestate): Move {
        //if count 1 just return random
        if (gamestate.rounds.size==0){
            return moves.shuffled().first()
        }
        //check if they or us have used a dynamite last round
        countDynamites(gamestate)
        //if count 2 return random
        if (gamestate.rounds.size==1){
            return moves.shuffled().first()
        }
        //update the matrices
        updateMatricies(gamestate)
        //get their next move probabilities
        val nextMoveProbs = getTheirNextMove(gamestate)
        // get our nex move
        return getOurNextMove(nextMoveProbs)

    }

    init {
        ourDynamitesPlayed = 0
        theirDynamitesPlayed = 0
        moves = arrayListOf(Move.R, Move.P, Move.S, Move.D, Move.W)
        moveBasedOnMyPrev = mutableMapOf()
        //make matrices
        initMatrices()
        println("Started new match")
    }

    fun initMatrices(){
        for (move in moves) {
            moveBasedOnMyPrev[move] =
                mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
        }
        for (move in moves) {
            moveBasedOnTheirPrev[move] =
                mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
        }
    }

    private fun countDynamites(gamestate: Gamestate){
        val my = gamestate.rounds.last().p1
        val their = gamestate.rounds.last().p2
        if (my == Move.D){
            ourDynamitesPlayed+=1
        }
        if (their == Move.D){
            theirDynamitesPlayed+=1
            //print("D")
        }
    }

    private fun updateMatricies(gamestate: Gamestate) {
        updateMatriciesStrat2(gamestate)
        updateMatriciesStrat3(gamestate)
    }

    private fun updateMatriciesStrat2(gamestate: Gamestate) {
        val first = gamestate.rounds[gamestate.rounds.size - 2].p1
        val second = gamestate.rounds[gamestate.rounds.size - 1].p2
        moveBasedOnMyPrev[first]?.set(second, moveBasedOnMyPrev[first]!![second]!! + 1)
    }

    private fun updateMatriciesStrat3(gamestate: Gamestate) {
        val first = gamestate.rounds[gamestate.rounds.size - 2].p2
        val second = gamestate.rounds[gamestate.rounds.size - 1].p2
        moveBasedOnMyPrev[first]?.set(second, moveBasedOnMyPrev[first]!![second]!! + 1)
    }

    private fun getTheirNextMove(gamestate: Gamestate): MutableMap<Move, Int> {
        val theirMoveProbs = mutableMapOf(Move.R to 0, Move.P to 0, Move.S to 0, Move.D to 0, Move.W to 0)
        val theirMoveProbsStrat2 = getTheirNextMoveStrat2(gamestate)
        val theirMoveProbsStrat3 = getTheirNextMoveStrat3(gamestate)
        for (key in theirMoveProbsStrat2.keys){
            theirMoveProbs[key] = theirMoveProbs[key]!! + theirMoveProbsStrat2[key]!!*1
        }
        for (key in theirMoveProbsStrat3.keys){
            theirMoveProbs[key] = theirMoveProbs[key]!! + theirMoveProbsStrat3[key]!!*1
        }
        return theirMoveProbs
    }

    private fun getTheirNextMoveStrat2(gamestate: Gamestate): MutableMap<Move, Int> {
        val myPrevMove = gamestate.rounds.last().p1
        return moveBasedOnMyPrev[myPrevMove]!!
    }

    private fun getTheirNextMoveStrat3(gamestate: Gamestate): MutableMap<Move, Int> {
        val myPrevMove = gamestate.rounds.last().p2
        return moveBasedOnTheirPrev[myPrevMove]!!
    }

    private fun getOurNextMove(theirMoveProbs: MutableMap<Move, Int>): Move {
        val ourMoveProbs = arrayListOf<Likelyhood>()
        if (theirDynamitesPlayed >= 100) {
            theirMoveProbs[Move.D] = 0
        }
        ourMoveProbs.add(Likelyhood(Move.R, theirMoveProbs[Move.S]!! + theirMoveProbs[Move.W]!!))
        ourMoveProbs.add(Likelyhood(Move.P, theirMoveProbs[Move.R]!! + theirMoveProbs[Move.W]!!))
        ourMoveProbs.add(Likelyhood(Move.S, theirMoveProbs[Move.P]!! + theirMoveProbs[Move.W]!!))
        ourMoveProbs.add(Likelyhood(Move.W, theirMoveProbs[Move.D]!!*2))
        if (ourDynamitesPlayed < 100) {
            ourMoveProbs.add(Likelyhood(Move.D, theirMoveProbs[Move.R]!! + theirMoveProbs[Move.P]!! + theirMoveProbs[Move.S]!! - theirMoveProbs[Move.W]!!))
        }
        ourMoveProbs.sortBy { it.prob }
        return ourMoveProbs.last().move
    }
}