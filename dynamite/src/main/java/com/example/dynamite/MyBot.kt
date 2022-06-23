package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move

data class Likelyhood(var move: Move, var prob: Double)

class MyBot : Bot {
    private val heuristicChange = 0.05
    private var heuristics = mutableMapOf(
        "basic2" to 0.01,
        "short2" to 4.0,
        "long2" to 2.0,
        "basic3" to 0.01,
        "short3" to 4.0,
        "long3" to 2.0,
        "drawS" to 3.0,
        "draw" to 3.0
    )
    private var heuristicsPrevGuess = mutableMapOf(
        "basic2" to Move.W,
        "short2" to Move.W,
        "long2" to Move.W,
        "basic3" to Move.W,
        "short3" to Move.W,
        "long3" to Move.W,
        "drawS" to Move.W,
        "draw" to Move.W
    )



    private val shortQSize = 3
    private val longQSize = 10

    private var draws = 0
    private var ourDynamitesPlayed = 0
    private var theirDynamitesPlayed = 0

    private var moves = arrayListOf(Move.R, Move.P, Move.S, Move.D, Move.W)

    private var moveBasedOnMyPrev = mutableMapOf<Move, MutableMap<Move, Double>>()
    private var moveBasedOnTheirPrev = mutableMapOf<Move, MutableMap<Move, Double>>()
    private var moveBasedOnMyPrevShort = mutableMapOf<Move, MutableMap<Move, Double>>()
    private var moveBasedOnTheirPrevShort = mutableMapOf<Move, MutableMap<Move, Double>>()
    private var moveBasedOnMyPrevLong = mutableMapOf<Move, MutableMap<Move, Double>>()
    private var moveBasedOnTheirPrevLong = mutableMapOf<Move, MutableMap<Move, Double>>()
    private var moveAfterDraw = mutableMapOf<Move, Double>()
    private var moveAfterSpecificDraw = mutableMapOf<Move, MutableMap<Move, Double>>()

    override fun makeMove(gamestate: Gamestate): Move {
        if (gamestate.rounds.size == 1000) {
            //println(heuristics)
        }
        //if count 1 just return random
        if (gamestate.rounds.size == 0) {
            return moves.shuffled().first()
        }
        //check if they or us have used a dynamite last round
        countDynamites(gamestate)
        //if count 2 return random
        if (gamestate.rounds.size == 1) {
            return moves.shuffled().first()
        }
        updateQueues(gamestate)
        //update the matrices
        updateMatricies(gamestate)
        //get their next move probabilities
        val nextMoveProbs = getTheirNextMove(gamestate)
        // get our nex move
        return getOurNextMove(nextMoveProbs)

    }

    init {
        heuristics = mutableMapOf(
            "basic2" to 0.02,
            "short2" to 4.0,
            "long2" to 2.0,
            "basic3" to 0.02,
            "short3" to 4.0,
            "long3" to 2.0,
            "drawS" to 3.0,
            "draw" to 3.0
        )
        heuristicsPrevGuess = mutableMapOf(
            "basic2" to Move.W,
            "short2" to Move.W,
            "long2" to Move.W,
            "basic3" to Move.W,
            "short3" to Move.W,
            "long3" to Move.W,
            "drawS" to Move.W,
            "draw" to Move.W
        )
        ourDynamitesPlayed = 0
        theirDynamitesPlayed = 0
        draws = 0
        moves = arrayListOf(Move.R, Move.P, Move.S, Move.D, Move.W)
        //make matrices
        initMatrices()
        println("Started new match")
    }

    fun initMatrices() {
        for (move in moves) {
            moveBasedOnMyPrev[move] =
                mutableMapOf(
                    Move.R to 0.0,
                    Move.P to 0.0,
                    Move.S to 0.0,
                    Move.D to 0.0,
                    Move.W to 0.0
                )
        }
        for (move in moves) {
            moveBasedOnTheirPrev[move] =
                mutableMapOf(
                    Move.R to 0.0,
                    Move.P to 0.0,
                    Move.S to 0.0,
                    Move.D to 0.0,
                    Move.W to 0.0
                )
        }
        for (move in moves) {
            moveBasedOnMyPrevShort[move] =
                mutableMapOf(
                    Move.R to 0.0,
                    Move.P to 0.0,
                    Move.S to 0.0,
                    Move.D to 0.0,
                    Move.W to 0.0
                )
        }
        for (move in moves) {
            moveBasedOnTheirPrevShort[move] =
                mutableMapOf(
                    Move.R to 0.0,
                    Move.P to 0.0,
                    Move.S to 0.0,
                    Move.D to 0.0,
                    Move.W to 0.0
                )
        }
        for (move in moves) {
            moveBasedOnMyPrevLong[move] =
                mutableMapOf(
                    Move.R to 0.0,
                    Move.P to 0.0,
                    Move.S to 0.0,
                    Move.D to 0.0,
                    Move.W to 0.0
                )
        }
        for (move in moves) {
            moveBasedOnTheirPrevLong[move] =
                mutableMapOf(
                    Move.R to 0.0,
                    Move.P to 0.0,
                    Move.S to 0.0,
                    Move.D to 0.0,
                    Move.W to 0.0
                )
        }
        for (move in moves) {
            moveAfterSpecificDraw[move] =
                mutableMapOf(
                    Move.R to 0.0,
                    Move.P to 0.0,
                    Move.S to 0.0,
                    Move.D to 0.0,
                    Move.W to 0.0
                )
        }
        moveAfterDraw = mutableMapOf(
            Move.R to 0.0,
            Move.P to 0.0,
            Move.S to 0.0,
            Move.D to 0.0,
            Move.W to 0.0
        )
    }

    private fun countDynamites(gamestate: Gamestate) {
        val my = gamestate.rounds.last().p1
        val their = gamestate.rounds.last().p2
        if (my == Move.D) {
            ourDynamitesPlayed += 1
        }
        if (their == Move.D) {
            theirDynamitesPlayed += 1
        }
    }

    private fun updateQueues(gamestate: Gamestate) {
        if (gamestate.rounds.size > shortQSize + 1) {
            val myfirst = gamestate.rounds[gamestate.rounds.size - 2 - shortQSize].p1
            val theirfirst = gamestate.rounds[gamestate.rounds.size - 2 - shortQSize].p2
            val second = gamestate.rounds[gamestate.rounds.size - 1 - shortQSize].p2
            moveBasedOnMyPrevShort[myfirst]?.set(
                second,
                moveBasedOnMyPrevShort[myfirst]!![second]!! - 1
            )
            moveBasedOnMyPrevShort[theirfirst]?.set(
                second,
                moveBasedOnMyPrevShort[theirfirst]!![second]!! - 1
            )
        }
        if (gamestate.rounds.size > longQSize + 1) {
            val myfirst = gamestate.rounds[gamestate.rounds.size - 2 - longQSize].p1
            val theirfirst = gamestate.rounds[gamestate.rounds.size - 2 - longQSize].p2
            val second = gamestate.rounds[gamestate.rounds.size - 1 - longQSize].p2
            moveBasedOnMyPrevLong[myfirst]?.set(
                second,
                moveBasedOnMyPrevLong[myfirst]!![second]!! - 1
            )
            moveBasedOnMyPrevLong[theirfirst]?.set(
                second,
                moveBasedOnMyPrevLong[theirfirst]!![second]!! - 1
            )
        }
    }

    private fun updateMatricies(gamestate: Gamestate) {
        updateMatriciesStrat2(gamestate)
        updateMatriciesStrat3(gamestate)
        updateDrawFreq(gamestate)
    }

    private fun updateMatriciesStrat2(gamestate: Gamestate) {
        val first = gamestate.rounds[gamestate.rounds.size - 2].p1
        val second = gamestate.rounds[gamestate.rounds.size - 1].p2
        moveBasedOnMyPrev[first]?.set(second, moveBasedOnMyPrev[first]!![second]!! + 1)
        moveBasedOnMyPrevShort[first]?.set(second, moveBasedOnMyPrevShort[first]!![second]!! + 1)
        moveBasedOnMyPrevLong[first]?.set(second, moveBasedOnMyPrevLong[first]!![second]!! + 1)
    }

    private fun updateMatriciesStrat3(gamestate: Gamestate) {
        val first = gamestate.rounds[gamestate.rounds.size - 2].p2
        val second = gamestate.rounds[gamestate.rounds.size - 1].p2
        moveBasedOnMyPrev[first]?.set(second, moveBasedOnMyPrev[first]!![second]!! + 1)
        moveBasedOnMyPrevShort[first]?.set(second, moveBasedOnMyPrevShort[first]!![second]!! + 1)
        moveBasedOnMyPrevLong[first]?.set(second, moveBasedOnMyPrevLong[first]!![second]!! + 1)

    }

    private fun updateDrawFreq(gamestate: Gamestate) {
        val my = gamestate.rounds[gamestate.rounds.size - 2].p1
        val their = gamestate.rounds[gamestate.rounds.size - 2].p2
        val theirAfter = gamestate.rounds[gamestate.rounds.size - 1].p2

        if (my == their) {
            moveAfterDraw[theirAfter] = moveAfterDraw[theirAfter]!! + 1
            moveAfterSpecificDraw[their]!![theirAfter] = moveAfterSpecificDraw[their]!![theirAfter]!! +1
        }

    }

    private fun getTheirNextMove(gamestate: Gamestate): MutableMap<Move, Double> {
        val theirMoveProbs =
            mutableMapOf(Move.R to 0.0, Move.P to 0.0, Move.S to 0.0, Move.D to 0.0, Move.W to 0.0)

        updateHeuristic("basic2",gamestate)
        updateHeuristic("basic3",gamestate)
        updateHeuristic("short2",gamestate)
        updateHeuristic("short3",gamestate)
        updateHeuristic("long2",gamestate)
        updateHeuristic("long3",gamestate)

        var theirMoveProbsStrat2 = getTheirNextMoveStrat2(gamestate)
        var theirMoveProbsStrat3 = getTheirNextMoveStrat3(gamestate)
        var nr = gamestate.rounds.size
        addPredictions(theirMoveProbs, "basic2", nr, theirMoveProbsStrat2)
        addPredictions(theirMoveProbs, "basic3", nr, theirMoveProbsStrat3)

        theirMoveProbsStrat2 = getTheirNextMoveStrat2Short(gamestate)
        theirMoveProbsStrat3 = getTheirNextMoveStrat3Short(gamestate)
        nr = shortQSize
        addPredictions(theirMoveProbs, "short2", nr, theirMoveProbsStrat2)
        addPredictions(theirMoveProbs, "short3", nr, theirMoveProbsStrat3)


        theirMoveProbsStrat2 = getTheirNextMoveStrat2Long(gamestate)
        theirMoveProbsStrat3 = getTheirNextMoveStrat3Long(gamestate)
        nr = longQSize
        addPredictions(theirMoveProbs, "long2", nr, theirMoveProbsStrat2)
        addPredictions(theirMoveProbs, "long3", nr, theirMoveProbsStrat3)


        if (lastDraw(gamestate, 2)){
            updateHeuristic("drawS",gamestate)
            updateHeuristic("draw",gamestate)
        }
        if (lastDraw(gamestate,1)) {
            val theirMoveProbsDraw = getTheirNextMoveDraw(gamestate)
            nr = draws
            addPredictions(theirMoveProbs, "drawS", nr, theirMoveProbsDraw)
            addPredictions(theirMoveProbs, "draw", nr, moveAfterDraw)
        }
        //println(theirMoveProbs)
        return theirMoveProbs
    }

    private fun addPredictions(out: MutableMap<Move, Double>, multName:String, divVal:Int, prediction: MutableMap<Move, Double>){
        var maxVal = 0.0
        var choice = Move.W
        for (key in prediction.keys){
            out[key] = out[key]!! + prediction[key]!!*heuristics[multName]!!/divVal
            if (maxVal<prediction[key]!!){
                maxVal=prediction[key]!!
                choice = key
            }
        }
        heuristicsPrevGuess[multName]=choice
    }

    private fun updateHeuristic(name:String, gamestate: Gamestate){
        val theirPrevMove = gamestate.rounds.last().p2
        if (theirPrevMove == heuristicsPrevGuess[name]){
            heuristics[name] = heuristics[name]!!*(1+heuristicChange)
        } else {
            heuristics[name] = heuristics[name]!!*(1-heuristicChange)
        }
    }

    private fun lastDraw(gamestate: Gamestate, count: Int): Boolean{
        draws += 1
        val my = gamestate.rounds[gamestate.rounds.size - count].p1
        val their = gamestate.rounds[gamestate.rounds.size - count].p2
        return my==their
    }

    private fun getTheirNextMoveDraw(gamestate: Gamestate): MutableMap<Move, Double> {
        val myPrevMove = gamestate.rounds.last().p1
        return moveAfterSpecificDraw[myPrevMove]!!
    }

    private fun getTheirNextMoveStrat2(gamestate: Gamestate): MutableMap<Move, Double> {
        val myPrevMove = gamestate.rounds.last().p1
        return moveBasedOnMyPrev[myPrevMove]!!
    }

    private fun getTheirNextMoveStrat3(gamestate: Gamestate): MutableMap<Move, Double> {
        val myPrevMove = gamestate.rounds.last().p2
        return moveBasedOnTheirPrev[myPrevMove]!!
    }

    private fun getTheirNextMoveStrat2Short(gamestate: Gamestate): MutableMap<Move, Double> {
        val myPrevMove = gamestate.rounds.last().p1
        return moveBasedOnMyPrevShort[myPrevMove]!!
    }

    private fun getTheirNextMoveStrat3Short(gamestate: Gamestate): MutableMap<Move, Double> {
        val myPrevMove = gamestate.rounds.last().p2
        return moveBasedOnTheirPrevShort[myPrevMove]!!
    }

    private fun getTheirNextMoveStrat2Long(gamestate: Gamestate): MutableMap<Move, Double> {
        val myPrevMove = gamestate.rounds.last().p1
        return moveBasedOnMyPrevLong[myPrevMove]!!
    }

    private fun getTheirNextMoveStrat3Long(gamestate: Gamestate): MutableMap<Move, Double> {
        val myPrevMove = gamestate.rounds.last().p2
        return moveBasedOnTheirPrevLong[myPrevMove]!!
    }

    private fun getOurNextMove(theirMoveProbs: MutableMap<Move, Double>): Move {
        val ourMoveProbs = arrayListOf<Likelyhood>()
        if (theirDynamitesPlayed >= 100) {
            theirMoveProbs[Move.D] = 0.0
        }
        ourMoveProbs.add(Likelyhood(Move.R, theirMoveProbs[Move.S]!! + theirMoveProbs[Move.W]!!))
        ourMoveProbs.add(Likelyhood(Move.P, theirMoveProbs[Move.R]!! + theirMoveProbs[Move.W]!!))
        ourMoveProbs.add(Likelyhood(Move.S, theirMoveProbs[Move.P]!! + theirMoveProbs[Move.W]!!))
        ourMoveProbs.add(Likelyhood(Move.W, theirMoveProbs[Move.D]!! * 2))
        if (ourDynamitesPlayed < 99) {
            ourMoveProbs.add(
                Likelyhood(
                    Move.D,
                    theirMoveProbs[Move.R]!! + theirMoveProbs[Move.P]!! + theirMoveProbs[Move.S]!! - theirMoveProbs[Move.W]!!
                )
            )
        }
        ourMoveProbs.sortBy { it.prob }

        return ourMoveProbs.last().move
    }
}