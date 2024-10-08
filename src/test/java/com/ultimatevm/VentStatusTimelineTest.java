package com.ultimatevm;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;

@Test()
public class VentStatusTimelineTest {
    int u = VentStatus.STARTING_VENT_VALUE;

    private void advanceTicks(VentStatusTimeline timeline, int numTicks) {
        for(int i = 0; i < numTicks; ++i) timeline.updateTick();
    }
    private int makeMoveBitState(int aMove, int bMove, int cMove) {
        int bitState = aMove | (bMove << 2) | (cMove << 4);
        return bitState << 6;
    }
    public void constructorTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        Assert.assertEquals(timeline.getCurrentTick(), 0);
        Assert.assertEquals(timeline.getCurrentStartingTick(), 0);
        Assert.assertEquals(timeline.getNumIdentifiedVents(), 0);
        Assert.assertNotNull(timeline.getTimeline());
        Assert.assertNotNull(timeline.getMovementVentStates());
        Assert.assertNotNull(timeline.getStabilityUpdateStates());
        Assert.assertNotNull(timeline.getIdentifiedVentTicks());
        Assert.assertNotNull(timeline.getIdentifiedVentStates());
        Assert.assertNull(timeline.getInitialState());
        for(int i = 0; i < StatusState.NUM_VENTS+1; ++i) {
            Assert.assertEquals(timeline.getIdentifiedVentTicks()[i], -1);
            Assert.assertNull(timeline.getIdentifiedVentStates()[i]);
        }
    }

    public void initializeTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        HashMap<Integer, StatusState> tickToMovementVentState = timeline.getMovementVentStates();

        timeline.addInitialState(new StatusState());
        timeline.addIdentifiedVentTick(new StatusState(), 1);
        timeline.addStabilityUpdateTick(new StatusState(), 23);
        timeline.updateTick();
        timeline.addMovementTick(new StatusState(), makeMoveBitState(0,0,0));

        //Everything should be reset back to starting values
        timeline.initialize();
        Assert.assertEquals(timeline.getCurrentTick(), 0);
        Assert.assertEquals(timeline.getCurrentStartingTick(), 0);
        Assert.assertEquals(timeline.getNumIdentifiedVents(), 0);
        Assert.assertNull(timeline.getInitialState());
        Assert.assertNotEquals(timelineEvents, timeline.getTimeline());
        Assert.assertNotEquals(tickToMovementVentState, timeline.getMovementVentStates());
        Assert.assertNotEquals(tickToStabilityUpdateState, timeline.getStabilityUpdateStates());
        for(int i = 0; i < StatusState.NUM_VENTS+1; ++i) {
            Assert.assertEquals(timeline.getIdentifiedVentTicks()[i], -1);
            Assert.assertNull(timeline.getIdentifiedVentStates()[i]);
        }
    }

    public void resetTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        HashMap<Integer, StatusState> tickToMovementVentState = timeline.getMovementVentStates();

        timeline.addInitialState(new StatusState());
        timeline.addIdentifiedVentTick(new StatusState(), 1);
        timeline.addMovementTick(new StatusState(), makeMoveBitState(0,0,0));
        timeline.updateTick();

        //Stored states, events and ticks should remain the same
        Assert.assertFalse(timeline.isHasReset());
        timeline.reset();
        Assert.assertTrue(timeline.isHasReset());
        Assert.assertEquals(timeline.getCurrentTick(), 1);
        Assert.assertEquals(timeline.getCurrentStartingTick(), 1);
        Assert.assertNull(timeline.getInitialState());
        Assert.assertEquals(timelineEvents, timeline.getTimeline());
        Assert.assertEquals(tickToMovementVentState, timeline.getMovementVentStates());
        Assert.assertEquals(tickToStabilityUpdateState, timeline.getStabilityUpdateStates());
        //Identified vents and states should be cleared
        Assert.assertEquals(timeline.getNumIdentifiedVents(), 0);
        for(int i = 0; i < StatusState.NUM_VENTS+1; ++i) {
            Assert.assertEquals(timeline.getIdentifiedVentTicks()[i], -1);
            Assert.assertNull(timeline.getIdentifiedVentStates()[i]);
        }
    }

    public void addInitialStateTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state1 = new StatusState();

        //Should successfully be added
        Assert.assertTrue(timeline.addInitialState(state1));
        StatusState addedState = timeline.getInitialState();
        Assert.assertNotNull(addedState);
        //New copied instance should be added
        Assert.assertNotEquals(addedState, state1);
        //Should fail if a second initial state is added since one exists
        Assert.assertFalse(timeline.addInitialState(state1));
    }

    public void addIdentifiedVentTickTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        timeline.addInitialState(new StatusState());
        int[] timelineEvents = timeline.getTimeline();
        int[] identifiedVentTicks = timeline.getIdentifiedVentTicks();
        StatusState[] identifiedVentStates = timeline.getIdentifiedVentStates();
        StatusState state1 = new StatusState();
        int onFlag = (1 << VentStatusTimeline.IDENTIFIED_VENT_FLAG);
        int garbageValue = 64;

        timeline.addIdentifiedVentTick(state1, 1 | garbageValue);
        Assert.assertEquals(timelineEvents[0], 1 | onFlag);
        Assert.assertEquals(identifiedVentTicks[0], 0);
        Assert.assertNotNull(identifiedVentStates[0]);
        Assert.assertNotEquals(identifiedVentStates[0], state1);
        Assert.assertEquals(timeline.getNumIdentifiedVents(), 1);

        timeline.addIdentifiedVentTick(state1, 2 | garbageValue);
        Assert.assertEquals(timelineEvents[0], 3 | onFlag);
        Assert.assertEquals(identifiedVentTicks[1], 0);
        Assert.assertNotNull(identifiedVentStates[1]);
        Assert.assertNotEquals(identifiedVentStates[1], state1);
        Assert.assertEquals(timeline.getNumIdentifiedVents(), 2);

        timeline.addIdentifiedVentTick(state1, 4 | garbageValue);
        Assert.assertEquals(timelineEvents[0], 7 | onFlag);
        Assert.assertEquals(identifiedVentTicks[2], 0);
        Assert.assertNotNull(identifiedVentStates[2]);
        Assert.assertNotEquals(identifiedVentStates[2], state1);
        Assert.assertEquals(timeline.getNumIdentifiedVents(), 3);
    }

    public void addIdentifiedVentTickInvalidTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        int[] identifiedVentTicks = timeline.getIdentifiedVentTicks();
        StatusState[] identifiedVentStates = timeline.getIdentifiedVentStates();
        StatusState state1 = new StatusState();

        timeline.addIdentifiedVentTick(state1, 64);
        Assert.assertEquals(timelineEvents[0], 0);
        for(int i = 0; i < StatusState.NUM_VENTS; ++i) {
            Assert.assertEquals(identifiedVentTicks[0], -1);
            Assert.assertNull(identifiedVentStates[0]);
        }
        Assert.assertEquals(timeline.getNumIdentifiedVents(), 0);
    }

    public void addIdentifiedVentTickReassignTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        timeline.addInitialState(new StatusState());
        int[] timelineEvents = timeline.getTimeline();
        int[] identifiedVentTicks = timeline.getIdentifiedVentTicks();
        StatusState[] identifiedVentStates = timeline.getIdentifiedVentStates();


        timeline.addIdentifiedVentTick(new StatusState(), 1);
        StatusState state = identifiedVentStates[0];
        timeline.updateTick();
        timeline.addIdentifiedVentTick(state, 1);

        //Values should be unchanged from last assignment
        Assert.assertEquals(timelineEvents[1], 0);
        Assert.assertEquals(identifiedVentTicks[0], 0);
        Assert.assertEquals(identifiedVentStates[0], state);
        Assert.assertEquals(timeline.getNumIdentifiedVents(), 1);
    }

    public void addDirectionChangeTickTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        int onFlag = (1 << VentStatusTimeline.DIRECTION_CHANGED_FLAG);

        timeline.addDirectionChangeTick((1 << 3) | 1);
        Assert.assertEquals(timelineEvents[0], (1 << 3) | onFlag);
        timeline.updateTick();
        timeline.addDirectionChangeTick((1 << 3) | 1);
        Assert.assertEquals(timelineEvents[1], (1 << 3) | onFlag);
    }

    public void addMovementTickTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        timeline.addInitialState(new StatusState());
        int[] timelineEvents = timeline.getTimeline();
        HashMap<Integer, StatusState> tickToMovementVentState = timeline.getMovementVentStates();
        StatusState state1 = new StatusState();
        int onFlag = (1 << VentStatusTimeline.MOVEMENT_UPDATE_FLAG);

        //This should fail to be added since 0 tick move is impossible
        timeline.addMovementTick(state1, makeMoveBitState(3,3,3));
        StatusState addedState = tickToMovementVentState.get(0);
        Assert.assertNull(addedState);

        //Should successfully be added
        advanceTicks(timeline, 10);
        int moveBitState = makeMoveBitState(3,3,3);
        timeline.addMovementTick(state1, moveBitState);
        addedState = tickToMovementVentState.get(10);
        Assert.assertNotNull(addedState);
        Assert.assertEquals(timelineEvents[10], onFlag | moveBitState);
        //New copied instance should be added
        Assert.assertNotEquals(addedState, state1);

        //Make sure movement bit state is added
        advanceTicks(timeline, 10);
        moveBitState = makeMoveBitState(2,2,2);
        timeline.addMovementTick(state1, moveBitState);
        Assert.assertEquals(timelineEvents[20], onFlag | moveBitState);
    }

    public void addStabilityUpdateTickTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        StatusState state1 = new StatusState();
        int onFlag = (1 << VentStatusTimeline.STABILITY_UPDATE_FLAG);

        timeline.addStabilityUpdateTick(state1, 0);
        //Should successfully be added
        StabilityUpdateInfo addedInfo = tickToStabilityUpdateState.get(0);
        Assert.assertNotNull(addedInfo);
        Assert.assertEquals(timelineEvents[0], onFlag);
        //New copied instance should be added
        Assert.assertNotEquals(addedInfo.getStabilityUpdateState(), state1);
    }

    public void addStabilityUpdateTickCalcTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        StatusState state1 = new StatusState();
        state1.updateVentStatus(new int[]{VentStatus.STARTING_VENT_VALUE,50, 50}, 0);
        timeline.addStabilityUpdateTick(state1, 10);

        //The added state should have a calculated estimated value
        StabilityUpdateInfo addedInfo = tickToStabilityUpdateState.get(0);
        StatusState addedState = addedInfo.getStabilityUpdateState();
        Assert.assertTrue(addedState.getVents()[0].isRangeDefined());
    }

    public void addEarthquakeEventTickTest() {
        //Earthquake event should be added and remove est move
        VentStatusTimeline MoveStartTimeline = new VentStatusTimeline();
        MoveStartTimeline.addInitialState(new StatusState());
        advanceTicks(MoveStartTimeline, 10);
        MoveStartTimeline.addMovementTick(new StatusState(), 0);
        advanceTicks(MoveStartTimeline, 10);
        Assert.assertTrue(MoveStartTimeline.addEstimatedMovementTick());
        MoveStartTimeline.addEarthquakeEventTick();
        Assert.assertEquals(MoveStartTimeline.getTimeline()[20], (1 << VentStatusTimeline.EARTHQUAKE_EVENT_FLAG));
    }

    public void addEstimatedMovementTickTest() {
        VentStatusTimeline MoveStartTimeline = new VentStatusTimeline();
        MoveStartTimeline.addInitialState(new StatusState());
        int addedEstMoveFlag = (1 << VentStatusTimeline.ESTIMATED_MOVEMENT_FLAG);

        //Should fail since estimated movements cannot occur on the starting tick
        Assert.assertFalse(MoveStartTimeline.addEstimatedMovementTick());

        //Should pass even though neither a movement or stability update has occured
        advanceTicks(MoveStartTimeline, 10);
        Assert.assertTrue(MoveStartTimeline.addEstimatedMovementTick());
        Assert.assertEquals(MoveStartTimeline.getTimeline()[10], addedEstMoveFlag);

        //Should pass even though no stability update
        advanceTicks(MoveStartTimeline, 10);
        MoveStartTimeline.addMovementTick(new StatusState(), 0);
        advanceTicks(MoveStartTimeline, 10);
        Assert.assertTrue(MoveStartTimeline.addEstimatedMovementTick());
        Assert.assertEquals(MoveStartTimeline.getTimeline()[30], addedEstMoveFlag);

        //Should pass even though no movement update
        VentStatusTimeline StabStartTimeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u, 50, 50}, 0);
        advanceTicks(StabStartTimeline, 10);
        StabStartTimeline.addStabilityUpdateTick(state, 20);
        Assert.assertTrue(StabStartTimeline.addEstimatedMovementTick());
        Assert.assertEquals(StabStartTimeline.getTimeline()[10], (1 << VentStatusTimeline.STABILITY_UPDATE_FLAG) | addedEstMoveFlag);

        //Should fail since an earthquake occured on the same tick
        advanceTicks(MoveStartTimeline, 10);
        MoveStartTimeline.addEarthquakeEventTick();
        Assert.assertFalse(MoveStartTimeline.addEstimatedMovementTick());
        Assert.assertEquals(MoveStartTimeline.getTimeline()[40], (1 << VentStatusTimeline.EARTHQUAKE_EVENT_FLAG));
    }

    public void clearMoveSkipEstimatedMoveTest() {
        int addedEstMoveFlag = (1 << VentStatusTimeline.ESTIMATED_MOVEMENT_FLAG);

        //Movement was skipped here remove the est move
        VentStatusTimeline MoveStartTimeline1 = new VentStatusTimeline();
        MoveStartTimeline1.addInitialState(new StatusState());
        advanceTicks(MoveStartTimeline1, 10);
        MoveStartTimeline1.addMovementTick(new StatusState(), 0);
        advanceTicks(MoveStartTimeline1, 10);
        Assert.assertTrue(MoveStartTimeline1.addEstimatedMovementTick());
        Assert.assertEquals(MoveStartTimeline1.getTimeline()[20], addedEstMoveFlag);
        advanceTicks(MoveStartTimeline1, 10);
        MoveStartTimeline1.addMovementTick(new StatusState(), 0);
        Assert.assertEquals(MoveStartTimeline1.getTimeline()[20], 0);

        //Movement was not skipped keep all est moves
        VentStatusTimeline MoveStartTimeline2 = new VentStatusTimeline();
        MoveStartTimeline2.addInitialState(new StatusState());
        advanceTicks(MoveStartTimeline2, 10);
        MoveStartTimeline2.addMovementTick(new StatusState(), 0);
        for(int i = 0; i < 4; ++i) {
            advanceTicks(MoveStartTimeline2, 10);
            Assert.assertTrue(MoveStartTimeline2.addEstimatedMovementTick());
        }
        advanceTicks(MoveStartTimeline2, 10);
        MoveStartTimeline2.addMovementTick(new StatusState(), 0);

        int tick = 10;
        for(int i = 0; i < 4; ++i) {
            tick += 10;
            Assert.assertEquals(MoveStartTimeline2.getTimeline()[tick], addedEstMoveFlag);
        }


        //Consec movement edge case; est move should be kept
        advanceTicks(MoveStartTimeline2, 10);
        MoveStartTimeline2.addMovementTick(new StatusState(), 0);

        tick = 10;
        for(int i = 0; i < 4; ++i) {
            tick += 10;
            Assert.assertEquals(MoveStartTimeline2.getTimeline()[tick], addedEstMoveFlag);
        }
    }

    public void fixPreviousEstimatedMovesTest() {
        int addedEstMoveFlag = (1 << VentStatusTimeline.ESTIMATED_MOVEMENT_FLAG);

        //Positioning of the est moves should be corrected
        VentStatusTimeline StabStartTimeline = new VentStatusTimeline();
        StabStartTimeline.addInitialState(new StatusState());
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u, 50, 50}, 0);
        advanceTicks(StabStartTimeline, 22);
        StabStartTimeline.addStabilityUpdateTick(state, 20);
        for(int i = 0; i < 3; ++i) {
            advanceTicks(StabStartTimeline, 10);
            Assert.assertTrue(StabStartTimeline.addEstimatedMovementTick());
        }
        advanceTicks(StabStartTimeline, 5);
        StabStartTimeline.addMovementTick(new StatusState(), 0);
        int oldTick = 22, newTick = 27;
        for(int i = 0; i < 3; ++i) {
            oldTick += 10;
            Assert.assertEquals(StabStartTimeline.getTimeline()[oldTick], 0);
            Assert.assertEquals(StabStartTimeline.getTimeline()[newTick], addedEstMoveFlag);
            newTick += 10;
        }
    }

    public void getTimelinePredictionStateIdentifyTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);

        //Vent B is identified
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,50,u}, 0);
        timeline.addIdentifiedVentTick(state, 2);

        StatusState resultState = timeline.getTimelinePredictionState();
        Assert.assertEquals(resultState.getVents()[0].getActualValue(), u);
        Assert.assertEquals(resultState.getVents()[1].getActualValue(), 50);
        Assert.assertEquals(resultState.getVents()[2].getActualValue(), u);

        //Vent A is identified
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{50,50,u}, 0);
        timeline.addIdentifiedVentTick(state, 1);

        resultState = timeline.getTimelinePredictionState();
        Assert.assertEquals(resultState.getVents()[0].getActualValue(), 50);
        Assert.assertEquals(resultState.getVents()[1].getActualValue(), 50);
        Assert.assertEquals(resultState.getVents()[2].getActualValue(), u);

        //Vent C is identified
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{50,50,50}, 0);
        timeline.addIdentifiedVentTick(state, 4);

        resultState = timeline.getTimelinePredictionState();
        Assert.assertEquals(resultState.getVents()[0].getActualValue(), 50);
        Assert.assertEquals(resultState.getVents()[1].getActualValue(), 50);
        Assert.assertEquals(resultState.getVents()[2].getActualValue(), 50);
    }

    public void getTimelinePredictionStateDirectionTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);

        advanceTicks(timeline, 5);
        timeline.addDirectionChangeTick(1 << 3);

        StatusState resultState = timeline.getTimelinePredictionState();
        Assert.assertEquals(resultState.getVents()[0].getDirection(), 1);
        Assert.assertEquals(resultState.getVents()[1].getDirection(), -1);
        Assert.assertEquals(resultState.getVents()[2].getDirection(), -1);

        advanceTicks(timeline, 5);
        timeline.addDirectionChangeTick(2 << 3);

        resultState = timeline.getTimelinePredictionState();
        Assert.assertEquals(resultState.getVents()[0].getDirection(), 1);
        Assert.assertEquals(resultState.getVents()[1].getDirection(), 1);
        Assert.assertEquals(resultState.getVents()[2].getDirection(), -1);

        advanceTicks(timeline, 5);
        timeline.addDirectionChangeTick(4 << 3);

        resultState = timeline.getTimelinePredictionState();
        Assert.assertEquals(resultState.getVents()[0].getDirection(), 1);
        Assert.assertEquals(resultState.getVents()[1].getDirection(), 1);
        Assert.assertEquals(resultState.getVents()[2].getDirection(), 1);

        advanceTicks(timeline, 5);
        timeline.addDirectionChangeTick(7 << 3);

        resultState = timeline.getTimelinePredictionState();
        Assert.assertEquals(resultState.getVents()[0].getDirection(), -1);
        Assert.assertEquals(resultState.getVents()[1].getDirection(), -1);
        Assert.assertEquals(resultState.getVents()[2].getDirection(), -1);
    }

    public void getTimelinePredictionStateMovementTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{50,u,u}, 0);
        timeline.addInitialState(state);

        for(int i = 0; i < 5; ++i) {
            advanceTicks(timeline, 10);
            state.updateVentStatus(new int[]{50-(i+1),u,u}, 0);
            timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));

            StatusState result = timeline.getTimelinePredictionState();
            Assert.assertEquals(result.getVents()[0].getActualValue(), 50-(i+1));
            Assert.assertEquals(result.getVents()[1].getActualValue(), u);
            Assert.assertEquals(result.getVents()[2].getActualValue(), u);
        }
    }

    public void getTimelinePredictionStateStabilityTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,75,75}, 0);
        timeline.addInitialState(state);

        advanceTicks(timeline, 25);
        timeline.addStabilityUpdateTick(state, 3);

        //Ensure we get a calculated value
        StatusState result = timeline.getTimelinePredictionState();
        VentStatus predictedVent = result.getVents()[0];
        Assert.assertEquals(predictedVent.getLowerBoundStart(), 35);
        Assert.assertEquals(predictedVent.getLowerBoundEnd(), 37);
        Assert.assertEquals(predictedVent.getUpperBoundStart(), 63);
        Assert.assertEquals(predictedVent.getUpperBoundEnd(), 65);
    }

    public void updatePreviousVentValuesTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);

        //Early Stability update
        advanceTicks(timeline, 20);
        timeline.addStabilityUpdateTick(state, 16);
        //Vent B is identified
        advanceTicks(timeline, 20);
        state.updateVentStatus(new int[]{u,75,u}, 0);
        timeline.addIdentifiedVentTick(state, 2);
        //A's direction was changed
        advanceTicks(timeline, 1);
        timeline.addDirectionChangeTick(1 << 3);
        //Do movement ticks
        for(int i = 0; i < 2; ++i) {
            state.updateVentStatus(new int[]{u,74-i,u}, 1);
            timeline.addMovementTick(state, makeMoveBitState(3, 1, 3));
            //Do same tick stability update
            if(i == 0) timeline.addStabilityUpdateTick(state, 16);
            advanceTicks(timeline, 10);
        }
        //Set earthquake and movement skip
        timeline.addEarthquakeEventTick();
        advanceTicks(timeline, 10);
        //A's direction was changed
        advanceTicks(timeline, 1);
        timeline.addDirectionChangeTick(1 << 3);
        //Vent A is identified
        advanceTicks(timeline, 2);
        state.updateVentStatus(new int[]{50,73,u}, 0);
        timeline.addIdentifiedVentTick(state, 1);

        //Verify results
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        HashMap<Integer, StatusState> tickToMovementVentState = timeline.getMovementVentStates();
        //Early Stability update should remain unchanged
        StatusState tick20StabState = tickToStabilityUpdateState.get(20).getStabilityUpdateState();
        Assert.assertEquals(tick20StabState.getVents()[0].getActualValue(), u);
        //Movement ticks should have the correct values
        int validMovementBit = makeMoveBitState(1, 1, 3);
        Assert.assertEquals(tickToMovementVentState.get(41).getVents()[0].getActualValue(), 49);
        Assert.assertEquals(timelineEvents[41] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(51).getVents()[0].getActualValue(), 50);
        Assert.assertEquals(timelineEvents[51] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        //Second stability update will have a value
        StatusState tick41StabState = tickToStabilityUpdateState.get(41).getStabilityUpdateState();
        Assert.assertEquals(tick41StabState.getVents()[0].getActualValue(), 49);
        //it should also have a new estimated value
        Assert.assertTrue(tick41StabState.getVents()[2].isRangeDefined());
    }

    public void updatePreviousVentValuesOnMovementTickTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);

        //Stability update
        advanceTicks(timeline, 20);
        timeline.addStabilityUpdateTick(state, 23);
        //Vents A B is identified
        advanceTicks(timeline, 1);
        state.updateVentStatus(new int[]{50,50,u}, 0);
        timeline.addIdentifiedVentTick(state, 3);

        //Stability update will not update since there are no movement ticks
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        StatusState tick20StabState = tickToStabilityUpdateState.get(20).getStabilityUpdateState();
        Assert.assertEquals(tick20StabState.getVents()[0].getActualValue(), u);
        Assert.assertEquals(tick20StabState.getVents()[1].getActualValue(), u);
        Assert.assertEquals(tick20StabState.getVents()[2].getActualValue(), u);

        //Do movement tick
        advanceTicks(timeline, 1);
        state.updateVentStatus(new int[]{49,50,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));

        //Stability update will be updated with new values
        Assert.assertEquals(tick20StabState.getVents()[0].getActualValue(), 50);
        Assert.assertEquals(tick20StabState.getVents()[1].getActualValue(), 50);
        Assert.assertEquals(tick20StabState.getVents()[2].getActualValue(), u);
    }

    public void updatePreviousVentValuesMissingMovementTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);

        //1st Stability update
        advanceTicks(timeline, 20);
        timeline.addStabilityUpdateTick(state, 19);
        //Vents B is identified
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,60,u}, 0);
        timeline.addIdentifiedVentTick(state, 2);
        //Do movement tick
        advanceTicks(timeline, 4);
        state.updateVentStatus(new int[]{u,59,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 1, 3));

        //1st Stability update should be changed
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        StatusState tick20StabState = tickToStabilityUpdateState.get(20).getStabilityUpdateState();
        Assert.assertEquals(tick20StabState.getVents()[0].getActualValue(), u);
        Assert.assertEquals(tick20StabState.getVents()[1].getActualValue(), 60);
        Assert.assertEquals(tick20StabState.getVents()[2].getActualValue(), u);

        //Skipped movement updates due to a freeze
        advanceTicks(timeline, 30);
        state.updateVentStatus(new int[]{u,58,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 1, 3));
        //Reachable Stability update
        advanceTicks(timeline, 10);
        timeline.addStabilityUpdateTick(state, 18);
        //Vent A is identified
        advanceTicks(timeline, 2);
        state.updateVentStatus(new int[]{55,58,u}, 0);
        timeline.addIdentifiedVentTick(state, 1);

        //1st Stability update should be the same as before - skipped movement updates
        Assert.assertEquals(tick20StabState.getVents()[0].getActualValue(), u);
        Assert.assertEquals(tick20StabState.getVents()[1].getActualValue(), 60);
        Assert.assertEquals(tick20StabState.getVents()[2].getActualValue(), u);
        //2nd Stability update should be changed
        StatusState tick69StabState = tickToStabilityUpdateState.get(69).getStabilityUpdateState();
        Assert.assertEquals(tick69StabState.getVents()[0].getActualValue(), 55);
        Assert.assertEquals(tick69StabState.getVents()[1].getActualValue(), 58);
        Assert.assertEquals(tick69StabState.getVents()[2].getActualValue(), u);
        Assert.assertTrue(tick69StabState.getVents()[2].isRangeDefined());
    }

    public void updatePreviousVentValuesReverseFailTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 7);
        timeline.addInitialState(state);
        int validMoveBitState = makeMoveBitState(1, 3, 3);

        //Vent A is identified
        advanceTicks(timeline, 38);
        state.updateVentStatus(new int[]{53,u,u}, 7);
        timeline.addIdentifiedVentTick(state, 1);
        //Do movement tick
        advanceTicks(timeline, 2);
        state.updateVentStatus(new int[]{54,u,u}, 7);
        timeline.addMovementTick(state, validMoveBitState);
        //Do movement tick + stability update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{55,u,u}, 7);
        timeline.addMovementTick(state, validMoveBitState);
        timeline.addStabilityUpdateTick(state, 18);
        //Identification + movement tick
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{56,41,u}, 7);
        timeline.addMovementTick(state, validMoveBitState);
        timeline.addIdentifiedVentTick(state, 2);


        //1st Stability update should remain the same due to reverse move fail
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        StatusState tick50StabState = tickToStabilityUpdateState.get(50).getStabilityUpdateState();
        Assert.assertEquals(tick50StabState.getVents()[0].getActualValue(), 55);
        Assert.assertEquals(tick50StabState.getVents()[1].getActualValue(), u);
        Assert.assertEquals(tick50StabState.getVents()[2].getActualValue(), u);
        //Movement ticks should also remain the same
        HashMap<Integer, StatusState> tickToMovementVentState = timeline.getMovementVentStates();
        Assert.assertEquals(tickToMovementVentState.get(40).getVents()[1].getActualValue(), u);
        Assert.assertEquals(timelineEvents[40] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMoveBitState);
        Assert.assertEquals(tickToMovementVentState.get(50).getVents()[1].getActualValue(), u);
        Assert.assertEquals(timelineEvents[50] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMoveBitState);
        Assert.assertEquals(tickToMovementVentState.get(60).getVents()[1].getActualValue(), 41);
        Assert.assertEquals(timelineEvents[60] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMoveBitState);
    }

    public void getCurrentPredictionStateTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);


        //C range was lost
        //22: Early Stability update of 18
        advanceTicks(timeline, 22);
        timeline.addStabilityUpdateTick(state, 18);
        //34: A Vent was identified to be 58
        advanceTicks(timeline, 12);
        state.updateVentStatus(new int[]{58,u,u}, 0);
        timeline.addIdentifiedVentTick(state, 1);
        //37: Movement update
        advanceTicks(timeline, 3);
        state.updateVentStatus(new int[]{57,u,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //47: Same tick movement and stability update of 19
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{56,u,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        timeline.addStabilityUpdateTick(state, 19);
        //57: Same tick B Vent was identified to be 43 and movement
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{55,43,u}, 0);
        timeline.addIdentifiedVentTick(state, 2);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //67: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{54,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //77: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{53,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //87: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{52,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //97: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{51,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //107: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{50,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //117: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{49,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //127: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{48,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //137: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{47,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //147: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{46,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //157: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{45,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //167: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{44,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //177: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{43,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //187: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{42,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //197: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{41,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //207: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{40,43,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 0));
        //217: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{38,42,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 0));
        //227: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{36,41,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 0));
        //237: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{34,40,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 0));
        //247: Earthquake and movement skip
        advanceTicks(timeline, 10);
        timeline.addEarthquakeEventTick();
        //257: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{32,38,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //267: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{30,36,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //277: Earthquake and movement skip
        advanceTicks(timeline, 10);
        timeline.addEarthquakeEventTick();
        //287: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{28,34,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //292: Earthquake event
        advanceTicks(timeline, 5);
        timeline.addEarthquakeEventTick();
        //297: Movement update
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{26,32,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //307: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{24,30,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //308: Vent A direction change
        advanceTicks(timeline, 1);
        timeline.addDirectionChangeTick(1 << 3);
        //317: Movement update
        advanceTicks(timeline, 9);
        state.updateVentStatus(new int[]{26,28,u}, 1);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //327: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{28,26,u}, 1);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //337: Earthquake and movement skip
        advanceTicks(timeline, 10);
        timeline.addEarthquakeEventTick();
        //347: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{30,24,u}, 1);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //357: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{32,22,u}, 1);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //367: Earthquake and movement skip
        advanceTicks(timeline, 10);
        timeline.addEarthquakeEventTick();
        //377: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{34,20,u}, 1);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //387: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{36,18,u}, 1);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //397: Earthquake and movement skip
        advanceTicks(timeline, 10);
        timeline.addEarthquakeEventTick();
        //407: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{38,16,u}, 1);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //417: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{40,14,u}, 1);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 0));
        //422: Stability update of -1
        advanceTicks(timeline, 5);
        timeline.addStabilityUpdateTick(state, -1);

        //Verify Results - C should have an defined range
        StatusState predictedState = timeline.getCurrentPredictionState();
        Assert.assertTrue(predictedState.getVents()[2].isRangeDefined());
    }

    public void freezeClippingTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);

        //12: C Vent was identified to be 58
        advanceTicks(timeline, 12);
        state.updateVentStatus(new int[]{u,u,58}, 0);
        timeline.addIdentifiedVentTick(state, 4);
        //28: B Vent was identified to be 53
        advanceTicks(timeline, 16);
        state.updateVentStatus(new int[]{u,53,58}, 0);
        timeline.addIdentifiedVentTick(state, 2);
        //40: Stability update of 19 (+1)
        advanceTicks(timeline, 12);
        timeline.addStabilityUpdateTick(state, 19);
        //43: Movement update
        advanceTicks(timeline, 3);
        state.updateVentStatus(new int[]{u,52,58}, 0);
        timeline.addMovementTick(state, makeMoveBitState(0, 1, 0));

        //Should be freeze clipped here
        StatusState predictedState = timeline.getTimelinePredictionState();
        Assert.assertFalse(predictedState.getVents()[0].isRangeDefined());

        //Should not be freeze clipped here
        predictedState = timeline.getCurrentPredictionState();
        Assert.assertTrue(predictedState.getVents()[0].isRangeDefined());
    }

    //Safe - reverse movement tests
    public void reverseMovementATest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        HashMap<Integer, StatusState> tickToMovementVentState = timeline.getMovementVentStates();
        StatusState state = new StatusState();
        state.doVMReset();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);

        //Tick 5: Identify C Vent
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,u,70}, 0);
        timeline.addIdentifiedVentTick(state, 4);
        state = timeline.getTimelinePredictionState();
        //Tick 8: Movement tick
        advanceTicks(timeline, 3);
        state.updateVentStatus(new int[]{u,u,68}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 3, 2));
        state = timeline.getTimelinePredictionState();
        //Tick 18: Movement tick
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,u,66}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 3, 2));
        state = timeline.getTimelinePredictionState();
        //Tick 23: Stability update
        advanceTicks(timeline, 5);
        timeline.addStabilityUpdateTick(state, -1);
        state = timeline.getTimelinePredictionState();
        //Tick 28: Movement tick
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,u,64}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 3, 2));
        state = timeline.getTimelinePredictionState();
        //Tick 38: Movement tick
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,u,62}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 3, 2));
        state = timeline.getTimelinePredictionState();
        //Tick 48: Same tick movement stability update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,u,60}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 3, 2));
        timeline.addStabilityUpdateTick(state, -3);
        state = timeline.getTimelinePredictionState();
        //Tick 53: Identify A Vent
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{14,u,60}, 0);
        timeline.addIdentifiedVentTick(state, 1);
        state = timeline.getTimelinePredictionState();

        //Ensure we get an accurate estimate for B
        Assert.assertEquals(state.getVents()[1].getLowerBoundStart(), 10);
        Assert.assertEquals(state.getVents()[1].getLowerBoundEnd(), 12);
        Assert.assertEquals(state.getVents()[1].getUpperBoundStart(), 10);
        Assert.assertEquals(state.getVents()[1].getUpperBoundEnd(), 12);
        //Check that all values reversed to movement states correctly
        int validMovementBit = makeMoveBitState(2, 3, 2);
        Assert.assertEquals(tickToMovementVentState.get(8).getVents()[0].getActualValue(), 22);
        Assert.assertEquals(timelineEvents[8] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(18).getVents()[0].getActualValue(), 20);
        Assert.assertEquals(timelineEvents[18] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(28).getVents()[0].getActualValue(), 18);
        Assert.assertEquals(timelineEvents[28] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(38).getVents()[0].getActualValue(), 16);
        Assert.assertEquals(timelineEvents[38] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(48).getVents()[0].getActualValue(), 14);
        Assert.assertEquals(timelineEvents[48] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        //Check that all values revered to stability update states correctly
        Assert.assertEquals(tickToStabilityUpdateState.get(23).getStabilityUpdateState().getVents()[0].getActualValue(), 20);
        Assert.assertEquals(tickToStabilityUpdateState.get(48).getStabilityUpdateState().getVents()[0].getActualValue(), 14);
    }

    public void reverseMovementBTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        HashMap<Integer, StatusState> tickToMovementVentState = timeline.getMovementVentStates();
        StatusState state = new StatusState();
        state.doVMReset();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);

        //Tick 5: Identify C Vent
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,u,70}, 0);
        timeline.addIdentifiedVentTick(state, 4);
        state = timeline.getTimelinePredictionState();
        //Tick 8: Movement tick
        advanceTicks(timeline, 3);
        state.updateVentStatus(new int[]{u,u,68}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 3, 2));
        state = timeline.getTimelinePredictionState();
        //Tick 18: Movement tick
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,u,66}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 3, 2));
        state = timeline.getTimelinePredictionState();
        //Tick 23: Stability update
        advanceTicks(timeline, 5);
        timeline.addStabilityUpdateTick(state, -1);
        state = timeline.getTimelinePredictionState();
        //Tick 28: Movement tick
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,u,64}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 3, 2));
        state = timeline.getTimelinePredictionState();
        //Tick 38: Movement tick
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,u,62}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 3, 2));
        state = timeline.getTimelinePredictionState();
        //Tick 48: Same tick movement stability update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,u,60}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 3, 2));
        timeline.addStabilityUpdateTick(state, -3);
        state = timeline.getTimelinePredictionState();
        //Tick 53: Identify B Vent
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,14,60}, 0);
        timeline.addIdentifiedVentTick(state, 2);
        state = timeline.getTimelinePredictionState();

        //Ensure we get an accurate estimate for A
        Assert.assertEquals(state.getVents()[0].getLowerBoundStart(), 10);
        Assert.assertEquals(state.getVents()[0].getLowerBoundEnd(), 12);
        Assert.assertEquals(state.getVents()[0].getUpperBoundStart(), 10);
        Assert.assertEquals(state.getVents()[0].getUpperBoundEnd(), 12);
        //Check that all values reversed to movement states correctly
        int validMovementBit = makeMoveBitState(3, 2, 2);
        Assert.assertEquals(tickToMovementVentState.get(8).getVents()[1].getActualValue(), 22);
        Assert.assertEquals(timelineEvents[8] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(18).getVents()[1].getActualValue(), 20);
        Assert.assertEquals(timelineEvents[18] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(28).getVents()[1].getActualValue(), 18);
        Assert.assertEquals(timelineEvents[28] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(38).getVents()[1].getActualValue(), 16);
        Assert.assertEquals(timelineEvents[38] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(48).getVents()[1].getActualValue(), 14);
        Assert.assertEquals(timelineEvents[48] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        //Check that all values revered to stability update states correctly
        Assert.assertEquals(tickToStabilityUpdateState.get(23).getStabilityUpdateState().getVents()[1].getActualValue(), 20);
        Assert.assertEquals(tickToStabilityUpdateState.get(48).getStabilityUpdateState().getVents()[1].getActualValue(), 14);
    }

    public void reverseMovementCTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        HashMap<Integer, StatusState> tickToMovementVentState = timeline.getMovementVentStates();
        StatusState state = new StatusState();
        state.doVMReset();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);

        //Tick 5: Identify B Vent
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,70,u}, 0);
        timeline.addIdentifiedVentTick(state, 2);
        state = timeline.getTimelinePredictionState();
        //Tick 8: Movement tick
        advanceTicks(timeline, 3);
        state.updateVentStatus(new int[]{u,68,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 2, 3));
        state = timeline.getTimelinePredictionState();
        //Tick 18: Movement tick
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,66,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 2, 3));
        state = timeline.getTimelinePredictionState();
        //Tick 23: Stability update
        advanceTicks(timeline, 5);
        timeline.addStabilityUpdateTick(state, -1);
        state = timeline.getTimelinePredictionState();
        //Tick 28: Movement tick
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,64,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 2, 3));
        state = timeline.getTimelinePredictionState();
        //Tick 38: Movement tick
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,62,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 2, 3));
        state = timeline.getTimelinePredictionState();
        //Tick 48: Same tick movement stability update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,60,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 2, 3));
        timeline.addStabilityUpdateTick(state, -3);
        state = timeline.getTimelinePredictionState();
        //Tick 53: Identify C Vent
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,60,14}, 0);
        timeline.addIdentifiedVentTick(state, 4);
        state = timeline.getTimelinePredictionState();

        //Ensure we get an accurate estimate for A
        Assert.assertEquals(state.getVents()[0].getLowerBoundStart(), 10);
        Assert.assertEquals(state.getVents()[0].getLowerBoundEnd(), 12);
        Assert.assertEquals(state.getVents()[0].getUpperBoundStart(), 10);
        Assert.assertEquals(state.getVents()[0].getUpperBoundEnd(), 12);
        //Check that all values reversed to movement states correctly
        int validMovementBit = makeMoveBitState(3, 2, 2);
        Assert.assertEquals(tickToMovementVentState.get(8).getVents()[2].getActualValue(), 22);
        Assert.assertEquals(timelineEvents[8] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(18).getVents()[2].getActualValue(), 20);
        Assert.assertEquals(timelineEvents[18] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(28).getVents()[2].getActualValue(), 18);
        Assert.assertEquals(timelineEvents[28] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(38).getVents()[2].getActualValue(), 16);
        Assert.assertEquals(timelineEvents[38] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertEquals(tickToMovementVentState.get(48).getVents()[2].getActualValue(), 14);
        Assert.assertEquals(timelineEvents[48] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        //Check that all values revered to stability update states correctly
        Assert.assertEquals(tickToStabilityUpdateState.get(23).getStabilityUpdateState().getVents()[2].getActualValue(), 20);
        Assert.assertEquals(tickToStabilityUpdateState.get(48).getStabilityUpdateState().getVents()[2].getActualValue(), 14);
    }

    public void reverseMovementFreezeClipAccurateATest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        int[] timelineEvents = timeline.getTimeline();
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        HashMap<Integer, StatusState> tickToMovementVentState = timeline.getMovementVentStates();
        StatusState state = new StatusState();
        state.doVMReset();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);

        //Tick 5: Identify B Vent
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,70,u}, 0);
        timeline.addIdentifiedVentTick(state, 2);
        state = timeline.getTimelinePredictionState();
        //Tick 8: Movement tick
        advanceTicks(timeline, 3);
        state.updateVentStatus(new int[]{u,69,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 1, 3));
        state = timeline.getTimelinePredictionState();
        //Tick 18: Movement tick
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,68,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 1, 3));
        state = timeline.getTimelinePredictionState();
        //Tick 23: Stability update
        advanceTicks(timeline, 5);
        timeline.addStabilityUpdateTick(state, 4);
        state = timeline.getTimelinePredictionState();
        //Tick 28: Movement tick
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{u,67,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 1, 3));
        state = timeline.getTimelinePredictionState();
        //Tick 38: Movement tick
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,66,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 1, 3));
        state = timeline.getTimelinePredictionState();
        //Tick 48: Same tick movement stability update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,65,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 1, 3));
        timeline.addStabilityUpdateTick(state, 2);
        state = timeline.getTimelinePredictionState();
        //Tick 58: Movement tick - freeze clipped A found here
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{u,63,u}, 0);
        timeline.addMovementTick(state, makeMoveBitState(3, 2, 3));
        state = timeline.getTimelinePredictionState();

        //We should have a freeze clipped accurate A
        Assert.assertTrue(state.getVents()[0].isFreezeClipAccurate());
        //Ensure data is set properly
        Assert.assertNotNull(timeline.getIdentifiedVentStates()[3]);
        Assert.assertEquals(timeline.getIdentifiedVentTicks()[3], 58);
        //Ensure data isnt set a second time + we only reverse once
        StatusState freezeClipAState = timeline.getIdentifiedVentStates()[3];
        advanceTicks(timeline, 1);
        state = timeline.getTimelinePredictionState();
        Assert.assertEquals(timeline.getIdentifiedVentStates()[3], freezeClipAState);
        Assert.assertEquals(timeline.getIdentifiedVentTicks()[3], 58);

        //Check that all values reversed to movement states correctly
        int validMovementBit = makeMoveBitState(3, 1, 3);
        Assert.assertTrue(tickToMovementVentState.get(8).getVents()[0].isFreezeClipAccurate());
        Assert.assertEquals(tickToMovementVentState.get(8).getVents()[0].getLowerBoundStart(), 44);
        Assert.assertEquals(timelineEvents[8] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertTrue(tickToMovementVentState.get(18).getVents()[0].isFreezeClipAccurate());
        Assert.assertEquals(tickToMovementVentState.get(18).getVents()[0].getLowerBoundStart(), 43);
        Assert.assertEquals(timelineEvents[18] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertTrue(tickToMovementVentState.get(28).getVents()[0].isFreezeClipAccurate());
        Assert.assertEquals(tickToMovementVentState.get(28).getVents()[0].getLowerBoundStart(), 42);
        Assert.assertEquals(timelineEvents[28] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertTrue(tickToMovementVentState.get(38).getVents()[0].isFreezeClipAccurate());
        Assert.assertEquals(tickToMovementVentState.get(38).getVents()[0].getLowerBoundStart(), 41);
        Assert.assertEquals(timelineEvents[38] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertTrue(tickToMovementVentState.get(48).getVents()[0].isFreezeClipAccurate());
        Assert.assertEquals(tickToMovementVentState.get(48).getVents()[0].getLowerBoundStart(), 40);
        Assert.assertEquals(timelineEvents[48] & VentStatusTimeline.MOVEMENT_BIT_MASK, validMovementBit);
        Assert.assertTrue(tickToMovementVentState.get(58).getVents()[0].isFreezeClipAccurate());
        Assert.assertEquals(tickToMovementVentState.get(58).getVents()[0].getLowerBoundStart(), 38);
        Assert.assertEquals(timelineEvents[58] & VentStatusTimeline.MOVEMENT_BIT_MASK, makeMoveBitState(3, 2, 3));
        //Check that all values reversed to stability update states correctly
        Assert.assertTrue(tickToStabilityUpdateState.get(23).getStabilityUpdateState().getVents()[0].isFreezeClipAccurate());
        Assert.assertEquals(tickToStabilityUpdateState.get(23).getStabilityUpdateState().getVents()[0].getLowerBoundStart(), 43);
        Assert.assertTrue(tickToStabilityUpdateState.get(48).getStabilityUpdateState().getVents()[0].isFreezeClipAccurate());
        Assert.assertEquals(tickToStabilityUpdateState.get(48).getStabilityUpdateState().getVents()[0].getLowerBoundStart(), 40);
    }

    public void checkHalfSpaceInvalidKnownVentsTest() {
        //1-0 known vents mismatch - Half space should not be done
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 0);
        timeline.addInitialState(state);
        advanceTicks(timeline, 25);
        timeline.addStabilityUpdateTick(state, 21);
        advanceTicks(timeline, 12);
        state.updateVentStatus(new int[]{u,52,u}, 0);
        timeline.addIdentifiedVentTick(state, 2);
        advanceTicks(timeline, 13);
        timeline.addStabilityUpdateTick(state, 23);

        final int[] timelineEvents = timeline.getTimeline();
        Assert.assertEquals((timelineEvents[50] & (1 << VentStatusTimeline.HALF_SPACE_COMPLETED_FLAG)), 0);
    }

    public void getPointContributionInvalidDirectionChangeTest() {
        //A direction check
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,51,u}, 0);
        timeline.addInitialState(state);
        advanceTicks(timeline, 25);
        timeline.addStabilityUpdateTick(state, 21);
        //A's direction was changed
        advanceTicks(timeline, 8);
        timeline.addDirectionChangeTick(1 << 3);
        advanceTicks(timeline, 17);
        timeline.addStabilityUpdateTick(state, 23);

        //Should fail since a direction change occured!
        final int[] timelineEvents = timeline.getTimeline();
        Assert.assertEquals((timelineEvents[50] & (1 << VentStatusTimeline.HALF_SPACE_COMPLETED_FLAG)), 0);


        //C direction test
        VentStatusTimeline timeline2 = new VentStatusTimeline();
        timeline2.addInitialState(state);
        advanceTicks(timeline2, 25);
        timeline2.addStabilityUpdateTick(state, 21);
        //C's direction was changed
        advanceTicks(timeline2, 8);
        timeline2.addDirectionChangeTick(4 << 3);
        advanceTicks(timeline2, 17);
        timeline2.addStabilityUpdateTick(state, 23);

        //Should fail since a direction change occured!
        final int[] timelineEvents2 = timeline2.getTimeline();
        Assert.assertEquals((timelineEvents2[50] & (1 << VentStatusTimeline.HALF_SPACE_COMPLETED_FLAG)), 0);
    }

    public void completeHalfSpaceInvalidStabilityChangeDiffTest() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,51,u}, 0);
        timeline.addInitialState(state);
        advanceTicks(timeline, 25);
        timeline.addStabilityUpdateTick(state, 21);
        advanceTicks(timeline, 25);
        timeline.addStabilityUpdateTick(state, 22);

        //Should fail since stability hasnt changed enough!
        final int[] timelineEvents = timeline.getTimeline();
        Assert.assertEquals((timelineEvents[50] & (1 << VentStatusTimeline.HALF_SPACE_COMPLETED_FLAG)), 0);
    }

    public void completeHalfSpaceTest() {
//        VentStatusTimeline timeline = new VentStatusTimeline();
//        StatusState state = new StatusState();
//        state.updateVentStatus(new int[]{u,51,u}, 0);
//        timeline.addInitialState(state);
//        advanceTicks(timeline, 25);
//        timeline.addStabilityUpdateTick(state, 21);
//        advanceTicks(timeline, 25);
//        timeline.addStabilityUpdateTick(state, 22);
//        //B's direction was changed
//        advanceTicks(timeline, 8);
//        timeline.addDirectionChangeTick(2 << 3);
//        advanceTicks(timeline, 17);
//        timeline.addStabilityUpdateTick(state, 23);
//
//        //Half space should succeed here
//        final int[] timelineEvents = timeline.getTimeline();
//        Assert.assertTrue((timelineEvents[75] & (1 << VentStatusTimeline.HALF_SPACE_COMPLETED_FLAG)) != 0);
//        //A vent should be clipped properly
//        StatusState predictedState = timeline.getCurrentPredictionState();
//        Assert.assertEquals(predictedState.getVents()[0].getLowerBoundStart(), 47);
//        Assert.assertEquals(predictedState.getVents()[0].getUpperBoundStart(), 47);
//        Assert.assertEquals(predictedState.getVents()[0].getLowerBoundEnd(), 56);
//        Assert.assertEquals(predictedState.getVents()[0].getUpperBoundEnd(), 56);
    }

    public void halfSpaceClippingTest() {
//        VentStatusTimeline timeline = new VentStatusTimeline();
//        StatusState startingState = new StatusState();
//        startingState.updateVentStatus(new int[]{0,u,u}, 1);
//
//        //Assume both B and C are 25%
//        //Both B and C are fixed
//        timeline.initialize();
//        StatusState state = new StatusState(startingState);
//        timeline.addInitialState(state);
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{2,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{4,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 5);
//        timeline.addStabilityUpdateTick(state, -7);
//        advanceTicks(timeline, 5);
//        state.updateVentStatus(new int[]{6,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{8,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{10,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        timeline.addStabilityUpdateTick(state, -1);
//
//        StatusState predictedState = timeline.getCurrentPredictionState();
//        Assert.assertEquals(predictedState.getVents()[1].getLowerBoundStart(), 47);
//        Assert.assertEquals(predictedState.getVents()[1].getUpperBoundStart(), 47);
//        Assert.assertEquals(predictedState.getVents()[1].getLowerBoundEnd(), 93);
//        Assert.assertEquals(predictedState.getVents()[1].getUpperBoundEnd(), 93);
//        Assert.assertEquals(predictedState.getVents()[2].getLowerBoundStart(), 47);
//        Assert.assertEquals(predictedState.getVents()[2].getUpperBoundStart(), 47);
//        Assert.assertEquals(predictedState.getVents()[2].getLowerBoundEnd(), 93);
//        Assert.assertEquals(predictedState.getVents()[2].getUpperBoundEnd(), 93);
//
//
//        //Both B and C are not fixed
//        timeline.initialize();
//        state = new StatusState(startingState);
//        timeline.addInitialState(state);
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{2,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{4,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 5);
//        timeline.addStabilityUpdateTick(state, -7);
//        advanceTicks(timeline, 5);
//        state.updateVentStatus(new int[]{6,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{8,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{10,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        timeline.addStabilityUpdateTick(state, -9);
//
//        predictedState = timeline.getCurrentPredictionState();
//        Assert.assertEquals(predictedState.getVents()[1].getLowerBoundStart(), 0);
//        Assert.assertEquals(predictedState.getVents()[1].getUpperBoundStart(), 0);
//        Assert.assertEquals(predictedState.getVents()[1].getLowerBoundEnd(), 37);
//        Assert.assertEquals(predictedState.getVents()[1].getUpperBoundEnd(), 37);
//        Assert.assertEquals(predictedState.getVents()[2].getLowerBoundStart(), 0);
//        Assert.assertEquals(predictedState.getVents()[2].getUpperBoundStart(), 0);
//        Assert.assertEquals(predictedState.getVents()[2].getLowerBoundEnd(), 37);
//        Assert.assertEquals(predictedState.getVents()[2].getUpperBoundEnd(), 37);
//
//
//        //Either B or C are fixed the other one isnt
//        timeline.initialize();
//        state = new StatusState(startingState);
//        timeline.addInitialState(state);
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{2,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{4,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 5);
//        timeline.addStabilityUpdateTick(state, -7);
//        advanceTicks(timeline, 5);
//        state.updateVentStatus(new int[]{6,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{8,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        advanceTicks(timeline, 10);
//        state.updateVentStatus(new int[]{10,u,u}, 1);
//        timeline.addMovementTick(state, makeMoveBitState(2, 0, 0));
//        timeline.addStabilityUpdateTick(state, -5);
//
//        predictedState = timeline.getCurrentPredictionState();
//        Assert.assertEquals(predictedState.getVents()[1].getLowerBoundStart(), 0);
//        Assert.assertEquals(predictedState.getVents()[1].getUpperBoundStart(), 0);
//        Assert.assertEquals(predictedState.getVents()[1].getLowerBoundEnd(), 94);
//        Assert.assertEquals(predictedState.getVents()[1].getUpperBoundEnd(), 94);
//        Assert.assertEquals(predictedState.getVents()[2].getLowerBoundStart(), 0);
//        Assert.assertEquals(predictedState.getVents()[2].getUpperBoundStart(), 0);
//        Assert.assertEquals(predictedState.getVents()[2].getLowerBoundEnd(), 97);
//        Assert.assertEquals(predictedState.getVents()[2].getUpperBoundEnd(), 97);
    }


    public void sandbox() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 3);
        timeline.addInitialState(state);

        //Incorrect C was picked
        //24: Early Stability update
        advanceTicks(timeline, 24);
        timeline.addStabilityUpdateTick(state, 14);
        //33: A Vent was identified to be 66
        advanceTicks(timeline, 9);
        state.updateVentStatus(new int[]{66,u,u}, 3);
        timeline.addIdentifiedVentTick(state, 1);
        //39: Earthquake and movement skip
        advanceTicks(timeline, 6);
        timeline.addEarthquakeEventTick();
        //47: A's direction was changed
        advanceTicks(timeline, 8);
        timeline.addDirectionChangeTick(1 << 3);
        //49: Same tick movement and stability update of 11
        advanceTicks(timeline, 2);
        state.updateVentStatus(new int[]{64,u,u}, 2);
        timeline.addMovementTick(state, makeMoveBitState(2, 3, 3));
        timeline.addStabilityUpdateTick(state, 11);
        //59: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{62,u,u}, 2);
        timeline.addMovementTick(state, makeMoveBitState(2, 3, 3));
        //69: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{60,u,u}, 2);
        timeline.addMovementTick(state, makeMoveBitState(2, 3, 3));
        //74: Stability update of 10
        advanceTicks(timeline, 5);
        timeline.addStabilityUpdateTick(state, 10);
        //79: Movement update
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{58,u,u}, 2);
        timeline.addMovementTick(state, makeMoveBitState(2, 3, 3));
        //80: B Vent was identified to be 77
        advanceTicks(timeline, 1);
        state.updateVentStatus(new int[]{58,77,u}, 2);
        timeline.addIdentifiedVentTick(state, 2);

        //Verify results - lowerbound 30s was right answer
        HashMap<Integer, StabilityUpdateInfo> tickToStabilityUpdateState = timeline.getStabilityUpdateStates();
        HashMap<Integer, StatusState> tickToMovementVentState = timeline.getMovementVentStates();
        StatusState tick74StabState = tickToStabilityUpdateState.get(74).getStabilityUpdateState();
        StatusState predictedState = timeline.getCurrentPredictionState();

        //89: Movement update
        advanceTicks(timeline, 9);
        state.updateVentStatus(new int[]{57,78,u}, 2);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //99: Same tick movement and stability update of 10
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{56,79,u}, 2);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        timeline.addStabilityUpdateTick(state, 10);

        predictedState = timeline.getCurrentPredictionState();
        Assert.assertEquals(predictedState.getVents()[2].getLowerBoundStart(), 35);
        Assert.assertEquals(predictedState.getVents()[2].getLowerBoundEnd(), 38);
        Assert.assertEquals(predictedState.getVents()[2].getUpperBoundStart(), 35);
        Assert.assertEquals(predictedState.getVents()[2].getUpperBoundEnd(), 38);
    }

    public void sandbox2() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 4);
        timeline.addInitialState(state);

        //Initial Stability Update is a +1 mod!
        //22: Early Stability update of 17
        advanceTicks(timeline, 22);
        timeline.addStabilityUpdateTick(state, 17);
        //34: A Vent was identified to be 44
        advanceTicks(timeline, 12);
        state.updateVentStatus(new int[]{44,u,u}, 4);
        timeline.addIdentifiedVentTick(state, 1);
        //37: Movement update
        advanceTicks(timeline, 3);
        state.updateVentStatus(new int[]{43,u,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 3, 3));
        //47: Same tick movement and stability update of 16
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{42,u,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 3, 3));
        timeline.addStabilityUpdateTick(state, 16);
        //50: A's direction was changed
        advanceTicks(timeline, 3);
        timeline.addDirectionChangeTick(1 << 3);
        //57: Movement update
        advanceTicks(timeline, 7);
        state.updateVentStatus(new int[]{43,u,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 3, 3));
        //67: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{44,u,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 3, 3));
        //72: Stability update of 16
        advanceTicks(timeline, 5);
        timeline.addStabilityUpdateTick(state, 16);
        //77: Movement update
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{45,u,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 3, 3));
        //80: B Vent was identified to be 47
        advanceTicks(timeline, 3);
        state.updateVentStatus(new int[]{45,47,u}, 5);
        timeline.addIdentifiedVentTick(state, 2);
        //87: Movement update
        advanceTicks(timeline, 7);
        state.updateVentStatus(new int[]{46,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //97: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{47,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //107: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{48,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //117: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{49,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //127: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{50,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //137: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{51,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //147: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{52,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //157: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{53,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //167: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{54,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //177: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{55,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //187: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{56,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //197: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{57,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //207: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{58,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //217: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{59,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //227: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{60,47,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //237: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{62,46,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //247: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{64,45,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //257: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{66,44,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //267: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{68,43,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //275: A's direction was changed
        advanceTicks(timeline, 8);
        timeline.addDirectionChangeTick(1 << 3);
        //277: Movement update
        advanceTicks(timeline, 2);
        state.updateVentStatus(new int[]{66,42,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //287: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{64,41,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //297: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{62,40,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //307: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{60,38,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //317: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{58,36,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //327: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{57,35,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //337: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{56,34,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //347: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{55,33,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //357: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{54,32,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //367: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{53,31,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //377: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{52,30,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //387: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{51,29,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //397: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{50,28,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //407: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{49,27,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //417: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{48,26,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //427: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{47,25,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //437: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{46,24,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //447: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{45,23,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //457: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{44,22,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //467: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{43,21,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //472: Stability update of -2
        advanceTicks(timeline, 5);
        timeline.addStabilityUpdateTick(state, -2);

        //Verify Results - C is a single range
        StatusState predictedState = timeline.getCurrentPredictionState();
        Assert.assertTrue(predictedState.getVents()[2].isRangeDefined());
        Assert.assertFalse(predictedState.getVents()[2].isTwoSeperateValues());
    }

    public void sandbox3() {
        VentStatusTimeline timeline = new VentStatusTimeline();
        StatusState state = new StatusState();
        state.updateVentStatus(new int[]{u,u,u}, 5);
        timeline.addInitialState(state);


        //C range was lost
        //24: Early Stability update of 22
        advanceTicks(timeline, 24);
        timeline.addStabilityUpdateTick(state, 22);
        //34: A Vent was identified to be 52
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{52,u,u}, 5);
        timeline.addIdentifiedVentTick(state, 1);
        //39: Movement update
        advanceTicks(timeline, 5);
        state.updateVentStatus(new int[]{53,u,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 3, 3));
        //49: Same tick movement and stability update of 20
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{54,u,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 3, 3));
        timeline.addStabilityUpdateTick(state, 20);
        //59: Same tick B Vent was identified to be 46 and movement
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{55,46,u}, 5);
        timeline.addIdentifiedVentTick(state, 2);
        timeline.addMovementTick(state, makeMoveBitState(1, 3, 3));
        //69: Earthquake and movement skip
        advanceTicks(timeline, 10);
        timeline.addEarthquakeEventTick();
        //79: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{56,46,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //89: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{57,46,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //109: Movement update
        advanceTicks(timeline, 20);
        state.updateVentStatus(new int[]{58,46,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //119: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{59,46,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //129: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{60,46,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //139: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{62,45,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //149: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{64,44,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //159: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{66,43,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //169: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{68,42,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //179: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{70,41,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //189: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{72,40,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 1, 3));
        //199: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{74,38,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //209: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{76,36,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //219: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{78,34,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //229: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{80,32,u}, 5);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //231: Vent A direction change
        advanceTicks(timeline, 2);
        timeline.addDirectionChangeTick(1 << 3);
        //239: Movement update
        advanceTicks(timeline, 8);
        state.updateVentStatus(new int[]{78,30,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //249: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{76,28,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //259: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{74,26,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //269: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{72,24,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //279: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{70,22,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //289: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{68,20,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //299: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{66,18,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //309: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{64,16,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //319: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{62,14,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //329: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{60,12,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //339: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{58,10,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(2, 2, 3));
        //349: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{57,9,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //359: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{56,8,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //369: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{55,7,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //379: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{54,6,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //389: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{53,5,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //409: Movement update
        advanceTicks(timeline, 20);
        state.updateVentStatus(new int[]{52,4,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //419: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{51,3,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //429: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{50,2,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //439: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{49,1,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 1, 3));
        //449: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{48,0,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //459: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{47,0,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //469: Movement update
        advanceTicks(timeline, 10);
        state.updateVentStatus(new int[]{46,0,u}, 4);
        timeline.addMovementTick(state, makeMoveBitState(1, 0, 3));
        //474: Stability update of -1
        advanceTicks(timeline, 5);
        timeline.addStabilityUpdateTick(state, -1);

        //Verify Results - C should have an defined range
        StatusState predictedState = timeline.getCurrentPredictionState();
        Assert.assertTrue(predictedState.getVents()[2].isRangeDefined());
    }

}
