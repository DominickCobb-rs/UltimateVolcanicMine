package com.ultimatevm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class VentStatusTimeline {
    //Constants
    public static final int VENT_MOVE_TICK_TIME = 10;
    public static final int STABILITY_UPDATE_TICK_TIME = 25;
    public static final int VM_GAME_FULL_TIME = 1000;
    public static final int VM_GAME_RESET_TIME = 500;

    //Flags
    public static final int DIRECTION_CHANGED_FLAG = 16;
    public static final int IDENTIFIED_VENT_FLAG = DIRECTION_CHANGED_FLAG+1;
    public static final int MOVEMENT_UPDATE_FLAG = IDENTIFIED_VENT_FLAG+1;
    public static final int STABILITY_UPDATE_FLAG = MOVEMENT_UPDATE_FLAG+1;
    public static final int EARTHQUAKE_EVENT_FLAG = STABILITY_UPDATE_FLAG+1;
    public static final int ESTIMATED_MOVEMENT_FLAG = EARTHQUAKE_EVENT_FLAG+1;


    //Masks
    public static final int IDENTIFIED_BIT_MASK = 7;
    public static final int DIRECTION_CHANGED_BIT_MASK = 7 << 3;
    public static final int MOVEMENT_BIT_MASK = 63 << 6;
    //       |   move    | dir |  id |
    //0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0

    private int currentTick, startingTick;
    private int currentMovementTick, firstStabilityUpdateTick;
    private int[] timeline;
    private int[] identifiedVentTick;
    private StatusState[] identifiedVentStates;
    private int numIdentifiedVents;
    StatusState initialState;
    StabilityUpdateInfo initialStabInfo;
    HashMap<Integer, StatusState> tickToMovementVentState;
    HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState;

    public VentStatusTimeline() {
        initialize();
    }
    public void initialize() {
        currentTick = 0;
        timeline = new int[VM_GAME_FULL_TIME];
        tickToMovementVentState = new HashMap<>();
        tickToStabilityUpdateState = new HashMap<>();
        reset();
    }
    public void reset() {
        firstStabilityUpdateTick = Integer.MAX_VALUE;
        currentMovementTick = startingTick = currentTick;
        numIdentifiedVents = 0;
        initialState = null;
        initialStabInfo = null;
        identifiedVentTick = new int[StatusState.NUM_VENTS];
        identifiedVentStates = new StatusState[StatusState.NUM_VENTS];
        for(int i = 0; i < StatusState.NUM_VENTS; ++i) {
            identifiedVentTick[i] = -1;
            identifiedVentStates[i] = null;
        }
    }
    public boolean addInitialState(StatusState startingState) {
        //Only add initial state once for pre reset and post reset
        if(initialState != null) return false;
        initialState = new StatusState(startingState);
        return true;
    }
    public void addIdentifiedVentTick(StatusState currentState, int bitState) {
        int ventIndex = -1;
        for(int i = 0; i < StatusState.NUM_VENTS; ++i) {
            if(identifiedVentTick[i] != -1) continue;
            if ((bitState & (1 << i)) != 0) {
                timeline[currentTick] |= (1 << IDENTIFIED_VENT_FLAG);
                timeline[currentTick] |= bitState & IDENTIFIED_BIT_MASK;
                ++numIdentifiedVents;
                identifiedVentStates[i] = new StatusState(currentState);
                identifiedVentTick[i] = currentTick;
                ventIndex = i;
            }
        }

        if(numIdentifiedVents == 3 || ventIndex == -1) return;
        //Backtrack and fill out missing vent values
        updatePreviousVentValues(identifiedVentStates[ventIndex], currentTick);
    }
    private void updatePreviousVentValues(StatusState startingState, int tick) {
        StatusState curState = new StatusState(startingState);
        LinkedList<Integer> stabilityUpdateTicks = new LinkedList<>();
        int numTicksNoMovement = 0, futureMovementTick = Integer.MAX_VALUE;
        for(int i = tick; i >= startingTick; --i) {
            //Exit when there is a chain of missing movement updates
            if(numTicksNoMovement > (VENT_MOVE_TICK_TIME * 2)) break;

            if((timeline[i] & (1 << STABILITY_UPDATE_FLAG)) != 0) {
                //If future movement occured within 10 ticks we can process this now
                if(futureMovementTick - i <= VENT_MOVE_TICK_TIME) {
                    StabilityUpdateInfo stabilityInfo = tickToStabilityUpdateState.get(i);
                    stabilityInfo.updateVentValues(curState);
                    setInitialStabilityUpdateInfo(stabilityInfo);
                }
                //otherwise we have to process this during the previous movement update
                else stabilityUpdateTicks.addLast(i);
            }

            if((timeline[i] & (1 << MOVEMENT_UPDATE_FLAG)) != 0) {
                numTicksNoMovement = 0;
                futureMovementTick = i;
                //update the future stability state
                if(!stabilityUpdateTicks.isEmpty()) {
                    StabilityUpdateInfo stabilityInfo = tickToStabilityUpdateState.get(stabilityUpdateTicks.getFirst());
                    stabilityInfo.updateVentValues(curState);
                    setInitialStabilityUpdateInfo(stabilityInfo);
                    stabilityUpdateTicks.removeFirst();
                }
                //update the movement state
                StatusState movementState = tickToMovementVentState.get(i);
                movementState.setVentsEqualTo(curState);
                //exit if we can longer reverse the movement
                if(!reverseMovement(curState, i-1)) break;
            } else ++numTicksNoMovement;

            if(isEarthquakeDelayMovement(i)) numTicksNoMovement = 0;

            if((timeline[i] & (1 << DIRECTION_CHANGED_FLAG)) != 0) {
                //Change our direction if it occured this tick
                changeStateDirection(curState, i);
            }
        }
    }
    private void clearMoveSkipEstimatedMove() {
        int minTick = Math.max(startingTick, currentTick - (int)(VENT_MOVE_TICK_TIME * 2.5f));
        int prevEstMoveTick = Integer.MIN_VALUE, prevMoveTick = Integer.MIN_VALUE;
        for(int i = currentTick-1; i >= minTick; --i) {
            if((timeline[i] & (1 << ESTIMATED_MOVEMENT_FLAG)) != 0) {
                //exit if consecutive estimated movement (means no skip)
                if(prevEstMoveTick != Integer.MIN_VALUE) return;
                prevEstMoveTick = i;
            }
            if((timeline[i] & (1 << MOVEMENT_UPDATE_FLAG)) != 0) {
                //exit if consecutive movement (means no skip)
                if(prevMoveTick != Integer.MIN_VALUE) return;
                prevMoveTick = i;
            }
        }
        //Exit if either estimated or actual movement tick are not found
        if(prevEstMoveTick == Integer.MIN_VALUE || prevMoveTick == Integer.MIN_VALUE) return;
        //Exit if the previous movement tick is after the estimated one
        if(prevMoveTick > prevEstMoveTick) return;
        //remove estimated movement tick was added since a movement tick was skipped
        timeline[prevEstMoveTick] &= ~(1 << ESTIMATED_MOVEMENT_FLAG);
    }
    private void fixPreviousEstimatedMoves() {
        //If there was no stab update there is no est move to fix
        if(initialStabInfo == null) return;
        int updateTick = currentTick % VENT_MOVE_TICK_TIME;
        for(int i = currentTick-1; i >= startingTick; --i) {
            //Exit before the first stability update occured
            //(impossible for there to be any est moves)
            if(i < firstStabilityUpdateTick) break;
            //Clear estimated movement flag
            timeline[i] &= ~(1 << ESTIMATED_MOVEMENT_FLAG);
            if(i % VENT_MOVE_TICK_TIME == updateTick)
                addEstimatedMovementTick(i);
        }
    }

    public void addDirectionChangeTick(int bitState) {
        timeline[currentTick] |= (bitState & DIRECTION_CHANGED_BIT_MASK);
        timeline[currentTick] |= (1 << DIRECTION_CHANGED_FLAG);
    }
    public void addEarthquakeEventTick() {
        timeline[currentTick] |= (1 << EARTHQUAKE_EVENT_FLAG);
        //Clear estimated movement flag
        timeline[currentTick] &= ~(1 << ESTIMATED_MOVEMENT_FLAG);
    }
    public void addMovementTick(StatusState currentState, int movementBitState) {
        addNewMovementTickState(currentTick, currentState, movementBitState);
        clearMoveSkipEstimatedMove();
        //Update previous values on the very first movement update (likely after a vent check)
        if(currentMovementTick == startingTick) {
            fixPreviousEstimatedMoves();
            updatePreviousVentValues(currentState, currentTick);
        }
        currentMovementTick = currentTick;
    }
    public void addStabilityUpdateTick(StatusState currentState, int change) {
        addNewStabilityUpdateTickState(currentTick, currentState, change);
    }
    public boolean addEstimatedMovementTick() {
        //We can only add estimates if at least 1 movement or stability update occured
        if(currentMovementTick == startingTick && initialStabInfo == null) return false;
        return addEstimatedMovementTick(currentTick);
    }
    private boolean addEstimatedMovementTick(int tick) {
        //Estimated movements cannot occur same tick as an earthquake
        if((timeline[tick] & (1 << EARTHQUAKE_EVENT_FLAG)) != 0)
            return false;
        timeline[tick] |= (1 << ESTIMATED_MOVEMENT_FLAG);
        return true;
    }
    public StatusState getTimelinePredictionState() {
        LinkedList<StatusState> possibleStates = new LinkedList<>();
        StatusState predictedState = new StatusState(initialState);
        int previousMovementTick = 0;
        possibleStates.push(predictedState);
        for(int i = startingTick+1; i <= currentTick; ++i) {
            if((timeline[i] & (1 << IDENTIFIED_VENT_FLAG)) != 0) {
                int idFlags = timeline[i] & IDENTIFIED_BIT_MASK;
                Iterator<StatusState> iterator = possibleStates.descendingIterator();
                while (iterator.hasNext()) {
                    StatusState curState = iterator.next();
                    if ((idFlags & 1) != 0) {
                        curState.setVentEqualTo(identifiedVentStates[0], 0);
                    }
                    if ((idFlags & 2) != 0) {
                        curState.setVentEqualTo(identifiedVentStates[1], 1);
                    }
                    if ((idFlags & 4) != 0) {
                        curState.setVentEqualTo(identifiedVentStates[2], 2);
                    }
                }
            }
            if((timeline[i] & (1 << DIRECTION_CHANGED_FLAG)) != 0) {
                //Change our direction if it occured this tick
                Iterator<StatusState> iterator = possibleStates.descendingIterator();
                while (iterator.hasNext()) {
                    changeStateDirection(iterator.next(), i);
                }
            }
            if((timeline[i] & (1 << ESTIMATED_MOVEMENT_FLAG)) != 0) {
                StatusState newPossibility = new StatusState(possibleStates.getLast());
                //Only set ranges when there is not a simple movement skip
                if(i - previousMovementTick > VENT_MOVE_TICK_TIME)
                    newPossibility.setFreezeRanges(0);

                newPossibility.doFreezeClipping(0);

                newPossibility.updateVentMovement();
                possibleStates.addLast(newPossibility);
                //Set predicted state to the new up to date possibility
                //Only set if value wasnt freeze clipped
                if(newPossibility.areRangesDefined()) predictedState = newPossibility;
            }
            if((timeline[i] & (1 << MOVEMENT_UPDATE_FLAG)) != 0) {
                previousMovementTick = i;
                int moveBitState = timeline[i] & MOVEMENT_BIT_MASK;
                moveBitState >>= 6;
                Iterator<StatusState> iterator = possibleStates.descendingIterator();
                while (iterator.hasNext()) {
                    StatusState curState = iterator.next();
                    curState.setFreezeRanges(moveBitState);
                    curState.doFreezeClipping(moveBitState);

                    //Update our estimated vent values
                    curState.updateVentMovement();
                    syncWithMovementState(curState, i);
                }
            }
            if((timeline[i] & (1 << STABILITY_UPDATE_FLAG)) != 0) {
                Iterator<StatusState> iterator = possibleStates.descendingIterator();
                while (iterator.hasNext()) {
                    StatusState curState = iterator.next();
                    //Use stability updates to set/narrow our possible values
                    StabilityUpdateInfo stabilityInfo = tickToStabilityUpdateState.get(i);
                    if (stabilityInfo == initialStabInfo) {
                        if(curState.areRangesDefined()) stabilityInfo.updatePredictedState(curState);
                        else curState.mergePredictedRangesWith(initialStabInfo.getStabilityUpdateState());
                    }
                    else stabilityInfo.updatePredictedState(curState);
                }
                //Remove all invalid possibilities - always keep 1 state even if invalid
                iterator = possibleStates.descendingIterator();
                while (iterator.hasNext()) {
                    if(possibleStates.size() == 1) break;
                    StatusState curState = iterator.next();
                    if(!curState.areRangesDefined()) iterator.remove();
                }
                predictedState = possibleStates.getLast();
            }
        }
        return predictedState;
    }
    public StatusState getCurrentPredictionState() {
        return StabilityUpdateInfo.getPredictionState(initialStabInfo, this);
    }

    //Helpers
    private void addNewMovementTickState(int tick, StatusState currentState, int moveState) {
        StatusState newState = new StatusState(currentState);
        tickToMovementVentState.put(tick, newState);
        timeline[tick] |= (1 << MOVEMENT_UPDATE_FLAG);
        timeline[tick] |= moveState;
    }
    private void addNewStabilityUpdateTickState(int tick, StatusState currentState, int change) {
        StabilityUpdateInfo newInfo = new StabilityUpdateInfo(currentState, tick, change);
        tickToStabilityUpdateState.put(currentTick, newInfo);
        timeline[tick] |= (1 << STABILITY_UPDATE_FLAG);
        setInitialStabilityUpdateInfo(newInfo);
    }
    private void setInitialStabilityUpdateInfo(StabilityUpdateInfo info) {
        firstStabilityUpdateTick = Math.min(firstStabilityUpdateTick, currentTick);
        //Skip if no identified vents
        int infoIdentifiedVentCount = info.getStabilityUpdateState().getNumIdentifiedVents();
        if(infoIdentifiedVentCount == 0) return;
        if(initialStabInfo != null) {
            int ourIdentifiedVentCount = initialStabInfo.getStabilityUpdateState().getNumIdentifiedVents();
            if(ourIdentifiedVentCount > infoIdentifiedVentCount) return;
            else if(ourIdentifiedVentCount < infoIdentifiedVentCount) initialStabInfo = info;
            else {
                if (initialStabInfo.getTickTimeStamp() > info.getTickTimeStamp())
                    initialStabInfo = info;
            }
        }
        else initialStabInfo = info;
    }
    private void changeStateDirection(StatusState state, int tick) {
        int directionFlags = timeline[tick] & DIRECTION_CHANGED_BIT_MASK;
        directionFlags >>= 3;
        if((directionFlags & 1) != 0) state.getVents()[0].flipDirection();
        if((directionFlags & 2) != 0) state.getVents()[1].flipDirection();
        if((directionFlags & 4) != 0) state.getVents()[2].flipDirection();
    }
    private void syncWithMovementState(StatusState state, int tick) {
        StatusState moveState = tickToMovementVentState.get(tick);
        for(int i = 0; i < StatusState.NUM_VENTS; ++i) {
            if(!moveState.getVents()[i].isIdentified()) continue;
            state.setVentEqualTo(moveState, i);
        }
    }
    private boolean isEarthquakeDelayMovement(int tick) {
        if((timeline[tick] & (1 << EARTHQUAKE_EVENT_FLAG)) == 0) return false;

        //Check if next 10 ticks was a movement update
        if(tick + VENT_MOVE_TICK_TIME <= VM_GAME_FULL_TIME) {
            if ((timeline[tick + VENT_MOVE_TICK_TIME] & (1 << MOVEMENT_UPDATE_FLAG)) != 0)
                return true;
        }
        //Check if previous 10 ticks was a movement update
        if(tick - VENT_MOVE_TICK_TIME >= startingTick) {
            return (timeline[tick - VENT_MOVE_TICK_TIME] & (1 << MOVEMENT_UPDATE_FLAG)) != 0;
        }
        return false;
    }
    private boolean reverseMovement(StatusState curState, int tick) {
        //Check what vent values are known from the previous movement tick
        StatusState prevMoveTickState = null;
        int numTicksNoMovement = 0;
        for(int i = tick; i >= startingTick; --i) {
            //Exit when there is a chain of missing movement updates
            if(numTicksNoMovement > (VENT_MOVE_TICK_TIME * 2)) break;

            if((timeline[i] & (1 << IDENTIFIED_VENT_FLAG)) != 0) {
                int idFlags = timeline[i] & IDENTIFIED_BIT_MASK;
                if((idFlags & 1) != 0) {
                    prevMoveTickState = identifiedVentStates[0];
                }
                if((idFlags & 2) != 0) {
                    prevMoveTickState = identifiedVentStates[1];
                }
                if((idFlags & 4) != 0) {
                    prevMoveTickState = identifiedVentStates[2];
                }
                break;
            }

            if((timeline[i] & (1 << MOVEMENT_UPDATE_FLAG)) != 0) {
                prevMoveTickState = tickToMovementVentState.get(i);
                break;
            } else ++numTicksNoMovement;

            if(isEarthquakeDelayMovement(i)) numTicksNoMovement = 0;
        }

        //Set and mark known values
        int knownBitFlag = 0, unknownBitMask = 0;
        if(prevMoveTickState != null) {
            for(int i = 0; i < StatusState.NUM_VENTS; ++i) {
                VentStatus prevVent = prevMoveTickState.getVents()[i];
                VentStatus curVent = curState.getVents()[i];
                //Both identified we already know the reverse state
                if(curVent.isIdentified() && prevVent.isIdentified()) {
                    knownBitFlag |= (1 << i);
                    curState.setVentEqualTo(prevMoveTickState, i);
                }
                if(!curVent.isIdentified() && !prevVent.isIdentified()) {
                    knownBitFlag |= (1 << i);
                    unknownBitMask |= (1 << i);
                }
            }
        }
        //If the full previous state is known to us no need to reverse
        if(knownBitFlag == 7) return true;

        //TODO: Fix this code later to work with estimated ranges
        int result = curState.reverseMovement(knownBitFlag & (~unknownBitMask));
        //A failed to reverse always exit
        if(result == -1) return false;
        //B failed to reverse
        else if(result == -2) {
            //If only A is identified ignore this
            if(numIdentifiedVents == 1 && identifiedVentTick[0] != -1) return true;
            //Otherwise treat B, C, AB, AC, BC identification as failure
            return false;
        }
        //C failed to reverse
        else if(result == -3) {
            //Typically do not care unless its known and bounded
        }
        return true;
    }


    //Accessors
    public int getCurrentTick() { return currentTick; }
    public int getCurrentStartingTick() {return startingTick;}
    public int getNumIdentifiedVents() { return numIdentifiedVents; }
    public final int[] getTimeline() { return timeline; }
    public final int[] getIdentifiedVentTicks() { return identifiedVentTick; }
    public final StatusState[] getIdentifiedVentStates() { return identifiedVentStates; }
    public final StatusState getInitialState() { return initialState; }
    public final HashMap<Integer, StatusState> getMovementVentStates() { return tickToMovementVentState; }
    public final HashMap<Integer, StabilityUpdateInfo> getStabilityUpdateStates() { return tickToStabilityUpdateState; }

    //Modifiers
    public void updateTick() { ++currentTick; }

}
