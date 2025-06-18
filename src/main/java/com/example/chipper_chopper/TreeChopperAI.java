package com.example.chipper_chopper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TreeChopperAI {
    private static final Map<UUID, AIState> playerStates = new ConcurrentHashMap<>();
    private static final int SEARCH_RADIUS = 20; // Increased for better tree finding
    private static final int COLLECTION_RADIUS = 10; // Increased for better item collection
    private static final double MOVEMENT_SPEED = 0.3; 
    private static final int CHOP_COOLDOWN = 8; // Slightly faster
    private static final double REACH_DISTANCE = 4.5;
    private static final int MAX_COLLECTION_ATTEMPTS = 5; // Reduced to prevent item obsession
    private static final double ROTATION_SPEED = 2.5; // Smoother rotation
    private static final int LEAF_CLEAR_RADIUS = 4; // Slightly larger radius
    private static final double GROUND_LEVEL_OFFSET = -1.0;
    
    // ENHANCED INTELLIGENCE TIMEOUTS - Much more aggressive
    private static final int QUICK_TIMEOUT = 15; // 0.75 seconds for immediate decisions
    private static final int STUCK_THRESHOLD = 25; // 1.25 seconds to detect stuck
    private static final int AGGRESSIVE_TIMEOUT = 40; // 2 seconds for aggressive action
    private static final int ABANDON_THRESHOLD = 80; // 4 seconds before complete abandonment
    
    // NEW: Advanced Intelligence Tracking
    private static final Map<UUID, AdvancedIntelligence> playerIntelligence = new ConcurrentHashMap<>();
    
    // NEW: Intelligent Pattern Recognition
    private static final int PATTERN_DETECTION_THRESHOLD = 3; // Detect loops after 3 repetitions
    private static final int FORCED_EXPLORATION_RADIUS = 30; // Force exploration beyond normal radius
    private static final double MINIMUM_PROGRESS_DISTANCE = 1.0; // Must move at least 1 block to count as progress
    
    public static void start(PlayerEntity player) {
        if (player != null) {
            AIState state = new AIState();
            playerStates.put(player.getUuid(), state);
            playerIntelligence.put(player.getUuid(), new AdvancedIntelligence());
            ChipperChopperMod.LOGGER.info("Started AI for player: " + player.getName().getString());
        }
    }
    
    public static void stop(PlayerEntity player) {
        if (player != null) {
            playerStates.remove(player.getUuid());
            playerIntelligence.remove(player.getUuid());
            ChipperChopperMod.LOGGER.info("Stopped AI for player: " + player.getName().getString());
        }
    }
    
    public static boolean isActive(PlayerEntity player) {
        return player != null && playerStates.containsKey(player.getUuid());
    }
    
    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            AIState state = playerStates.get(player.getUuid());
            AdvancedIntelligence intelligence = playerIntelligence.get(player.getUuid());
            if (state != null && intelligence != null) {
                processAI(player, state, intelligence);
            }
        }
    }
    
    private static void processAI(ServerPlayerEntity player, AIState state, AdvancedIntelligence intelligence) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        // Increment tick counters
        state.ticksSinceLastAction++;
        if (state.chopCooldown > 0) {
            state.chopCooldown--;
        }
        
        // Update intelligence tracking
        intelligence.updatePositionHistory(playerPos);
        
        // CRITICAL: Check if we're completely stuck and need emergency intervention
        if (intelligence.isCompletelyStuck()) {
            ChipperChopperMod.LOGGER.info("Agent.Lumber: EMERGENCY - Player completely stuck, forcing reset");
            emergencyReset(state, intelligence);
            return;
        }
        
        switch (state.currentTask) {
            case IDLE:
                state.isMoving = false;
                state.targetRotation = null;
                state.thinkingState = AIThinkingState.SCANNING;
                
                // Enhanced tree finding with intelligence
                if (!findNearestTreeIntelligent(player, state, intelligence)) {
                    // If no trees found and we've been idle too long, expand search
                    if (state.ticksSinceLastAction > 100) { // 5 seconds
                        ChipperChopperMod.LOGGER.info("Agent.Lumber: Expanding search radius due to long idle time");
                        if (!findNearestTree(player, state, SEARCH_RADIUS * 2)) {
                            // Still no trees - try collecting items
                            if (state.collectionAttempts < MAX_COLLECTION_ATTEMPTS) {
                                if (collectNearbyItems(player, state)) {
                                    state.collectionAttempts++;
                                }
                            } else {
                                // Reset collection attempts after a while
                                if (state.ticksSinceLastAction > 200) {
                                    state.collectionAttempts = 0;
                                    ChipperChopperMod.LOGGER.info("Agent.Lumber: Resetting collection attempts");
                                }
                            }
                        }
                    }
                }
                break;
                
            case MOVING_TO_TREE:
                state.ticksSinceLastAction++;
                state.thinkingState = AIThinkingState.PATHFINDING;
                
                if (state.targetTree != null) {
                    double distance = player.getPos().distanceTo(Vec3d.ofCenter(state.targetTree));
                    
                    // Check if this target is in our blacklist
                    if (intelligence.isBlacklisted(state.targetTree)) {
                        ChipperChopperMod.LOGGER.info("Agent.Lumber: Target is blacklisted, finding new tree");
                        state.currentTask = AIState.Task.IDLE;
                        state.targetTree = null;
                        state.ticksSinceLastAction = 0;
                        break;
                    }
                    
                    // Quick timeout for movement issues
                    if (state.ticksSinceLastAction > QUICK_TIMEOUT && intelligence.hasMovementStagnated()) {
                        ChipperChopperMod.LOGGER.info("Agent.Lumber: Movement stagnation detected, finding alternative");
                        if (!findAlternativeApproach(player, state, intelligence)) {
                            intelligence.blacklistTarget(state.targetTree, "Movement stagnation");
                            state.currentTask = AIState.Task.IDLE;
                            state.targetTree = null;
                        }
                        state.ticksSinceLastAction = 0;
                        break;
                    }
                    
                    if (distance <= REACH_DISTANCE) {
                        if (shouldClearLeavesFirst(world, player.getBlockPos(), state.targetTree)) {
                            state.currentTask = AIState.Task.CLEARING_LEAVES;
                            state.ticksSinceLastAction = 0;
                            ChipperChopperMod.LOGGER.info("Need to clear leaves before chopping tree");
                        } else {
                            state.currentTask = AIState.Task.CHOPPING;
                            state.ticksSinceLastAction = 0;
                            state.isMoving = false;
                            state.consecutiveLineOfSightFailures = 0;
                            intelligence.recordSuccess("Reached tree for chopping");
                            ChipperChopperMod.LOGGER.info("Reached tree, starting to chop at: " + state.targetTree);
                            startLookingAt(player, state, state.targetTree);
                        }
                    } else if (distance > 50) {
                        intelligence.blacklistTarget(state.targetTree, "Too far away");
                        state.currentTask = AIState.Task.IDLE;
                        state.targetTree = null;
                        state.ticksSinceLastAction = 0;
                    } else {
                        state.isMoving = true;
                        if (state.targetRotation == null) {
                            startLookingAt(player, state, state.targetTree);
                        }
                        
                        // More aggressive timeout for movement
                        if (state.ticksSinceLastAction > AGGRESSIVE_TIMEOUT) {
                            ChipperChopperMod.LOGGER.info("Agent.Lumber: Aggressive timeout during movement, forcing progression");
                            if (distance < 8.0) {
                                state.currentTask = AIState.Task.CHOPPING;
                                state.ticksSinceLastAction = 0;
                                startLookingAt(player, state, state.targetTree);
                                intelligence.recordProgress("Forced progression to chopping");
                            } else {
                                intelligence.blacklistTarget(state.targetTree, "Movement timeout");
                                state.currentTask = AIState.Task.IDLE;
                                state.targetTree = null;
                                state.ticksSinceLastAction = 0;
                            }
                        }
                    }
                } else {
                    state.currentTask = AIState.Task.IDLE;
                    state.ticksSinceLastAction = 0;
                }
                break;
                
            case CLEARING_LEAVES:
                state.ticksSinceLastAction++;
                state.thinkingState = AIThinkingState.CLEARING_OBSTACLES;
                
                if (state.targetTree != null) {
                    // NEW: Detect if we're in a leaf-clearing loop
                    if (intelligence.isInLeafClearingLoop(state.targetTree)) {
                        ChipperChopperMod.LOGGER.info("Agent.Lumber: Detected leaf-clearing loop, forcing tree abandonment");
                        intelligence.blacklistTarget(state.targetTree, "Leaf-clearing loop detected");
                        intelligence.forcedExplorationMode = true; // Force exploration of new areas
                        state.currentTask = AIState.Task.IDLE;
                        state.targetTree = null;
                        state.currentLeafTarget = null;
                        state.ticksSinceLastAction = 0;
                        break;
                    }
                    
                    // Much more aggressive timeout - 1 second instead of 15
                    if (state.ticksSinceLastAction > 20) { // 1 second
                        ChipperChopperMod.LOGGER.info("Agent.Lumber: Ultra-fast leaf timeout, immediate tree abandonment");
                        
                        // Don't even try alternatives - immediately abandon
                        intelligence.blacklistTarget(state.targetTree, "Leaf clearing ultra-timeout");
                        intelligence.recordLeafClearingFailure(state.targetTree);
                        state.currentTask = AIState.Task.IDLE;
                        state.targetTree = null;
                        state.currentLeafTarget = null;
                        state.ticksSinceLastAction = 0;
                        break;
                    }
                    
                    // Find leaf target with intelligence
                    if (state.currentLeafTarget == null || state.ticksSinceLastAction % 3 == 0) { // Check even more frequently
                        BlockPos leafTarget = findObstructingLeaf(world, player.getBlockPos(), state.targetTree);
                        if (leafTarget != null && !intelligence.isProblematic(leafTarget)) {
                            state.currentLeafTarget = leafTarget;
                            startLookingAt(player, state, leafTarget);
                            intelligence.recordLeafClearingAttempt(state.targetTree, leafTarget);
                            ChipperChopperMod.LOGGER.info("Agent.Lumber: Targeting leaf obstacle: " + leafTarget);
                        } else if (leafTarget != null && intelligence.isProblematic(leafTarget)) {
                            ChipperChopperMod.LOGGER.info("Agent.Lumber: All accessible leaves are problematic, abandoning tree");
                            intelligence.blacklistTarget(state.targetTree, "All leaves problematic");
                            state.currentTask = AIState.Task.IDLE;
                            state.targetTree = null;
                            break;
                        } else {
                            // No obstructing leaves found - validate this is actually true
                            if (hasServerLineOfSight(world, player.getBlockPos(), state.targetTree)) {
                                state.currentTask = AIState.Task.CHOPPING;
                                state.currentLeafTarget = null;
                                state.thinkingState = AIThinkingState.CHOPPING;
                                ChipperChopperMod.LOGGER.info("Agent.Lumber: Path verified clear, resuming wood chopping");
                                intelligence.recordProgress("Leaf clearing validated complete");
                            } else {
                                // Claims no leaves but still no line of sight - something's wrong
                                ChipperChopperMod.LOGGER.info("Agent.Lumber: Line of sight still blocked despite no leaves found");
                                intelligence.blacklistTarget(state.targetTree, "Persistent obstruction");
                                state.currentTask = AIState.Task.IDLE;
                                state.targetTree = null;
                            }
                        }
                    }
                } else {
                    state.currentTask = AIState.Task.IDLE;
                    state.thinkingState = AIThinkingState.IDLE;
                }
                break;
                
            case CHOPPING:
                state.ticksSinceLastAction++;
                state.thinkingState = AIThinkingState.CHOPPING;
                
                if (state.targetTree != null && state.chopCooldown <= 0) {
                    // NEW: Check for intelligent upgrade loops (major improvement)
                    if (intelligence.isInIntelligentUpgradeLoop(state.targetTree)) {
                        ChipperChopperMod.LOGGER.info("Agent.Lumber: Detected intelligent upgrade loop, breaking cycle");
                        intelligence.blacklistTarget(state.targetTree, "Intelligent upgrade loop");
                        state.currentTask = AIState.Task.COLLECTING;
                        state.targetTree = null;
                        break;
                    }
                    
                    // Check if we should abandon due to repeated failures
                    if (intelligence.shouldAbandonTarget(state.targetTree)) {
                        ChipperChopperMod.LOGGER.info("Agent.Lumber: Intelligence system recommends abandoning target");
                        intelligence.blacklistTarget(state.targetTree, "Repeated failures");
                        state.currentTask = AIState.Task.COLLECTING;
                        state.targetTree = null;
                        break;
                    }
                    
                    // Less frequent rotation updates for stability
                    if (state.ticksSinceLastAction % 15 == 0) { // Every 0.75 seconds
                        updateRotation(player, state);
                    }
                    
                    if (isLogBlock(world.getBlockState(state.targetTree))) {
                        // Check for leaves - but be much smarter about it
                        if (shouldClearLeavesFirst(world, player.getBlockPos(), state.targetTree)) {
                            // NEW: Before going to leaf clearing, check if we've been stuck in this pattern
                            if (intelligence.hasRecentLeafClearingFailures(state.targetTree)) {
                                ChipperChopperMod.LOGGER.info("Agent.Lumber: Recent leaf clearing failures, skipping leaves and finding new target");
                                intelligence.blacklistTarget(state.targetTree, "Persistent leaf problems");
                                state.currentTask = AIState.Task.IDLE;
                                state.targetTree = null;
                                break;
                            }
                            
                            // Only try intelligent alternatives if we haven't been cycling
                            if (!intelligence.hasRecentlyTriedAlternatives(state.targetTree) && 
                                !intelligence.isInIntelligentUpgradeLoop(state.targetTree)) {
                                BlockPos intelligentTarget = findIntelligentTarget(world, player.getBlockPos(), state.targetTree, state);
                                if (intelligentTarget != null && !intelligentTarget.equals(state.targetTree)) {
                                    // Validate this target is actually better
                                    if (hasServerLineOfSight(world, player.getBlockPos(), intelligentTarget) &&
                                        !shouldClearLeavesFirst(world, player.getBlockPos(), intelligentTarget)) {
                                        ChipperChopperMod.LOGGER.info("Agent.Lumber: Validated better target: " + intelligentTarget);
                                        state.targetTree = intelligentTarget;
                                        startLookingAt(player, state, intelligentTarget);
                                        state.ticksSinceLastAction = 0;
                                        intelligence.recordIntelligentUpgrade(state.targetTree, intelligentTarget);
                                        break;
                                    }
                                }
                            }
                            
                            // Go to leaf clearing only as last resort
                            state.currentTask = AIState.Task.CLEARING_LEAVES;
                            state.currentLeafTarget = null;
                            state.thinkingState = AIThinkingState.CLEARING_OBSTACLES;
                            intelligence.markAlternativesTried(state.targetTree);
                            break;
                        }
                        
                        // Handle line of sight failures more aggressively
                        if (state.consecutiveLineOfSightFailures >= 2) { // Reduced from 3 to 2
                            ChipperChopperMod.LOGGER.info("Agent.Lumber: Quick LOS failure response");
                            
                            // Don't try alternatives if we've been cycling - just abandon
                            if (intelligence.hasRecentlyRepositioned(state.targetTree) || 
                                intelligence.isInIntelligentUpgradeLoop(state.targetTree)) {
                                ChipperChopperMod.LOGGER.info("Agent.Lumber: Recent cycling detected, abandoning problematic target");
                                intelligence.blacklistTarget(state.targetTree, "LOS failures after cycling");
                                state.currentTask = AIState.Task.IDLE;
                                state.targetTree = null;
                                break;
                            }
                            
                            // Try one alternative, but be strict about it
                            BlockPos intelligentTarget = findIntelligentTarget(world, player.getBlockPos(), state.targetTree, state);
                            if (intelligentTarget != null && !intelligentTarget.equals(state.targetTree) &&
                                hasServerLineOfSight(world, player.getBlockPos(), intelligentTarget)) {
                                state.targetTree = intelligentTarget;
                                startLookingAt(player, state, intelligentTarget);
                                state.ticksSinceLastAction = 0;
                                state.consecutiveLineOfSightFailures = 0;
                                intelligence.recordProgress("LOS failure recovery");
                                intelligence.markRepositioned(state.targetTree);
                                break;
                            } else {
                                // No good alternatives - abandon immediately
                                intelligence.blacklistTarget(state.targetTree, "LOS failures, no alternatives");
                                state.currentTask = AIState.Task.IDLE;
                                state.targetTree = null;
                                break;
                            }
                        }
                    } else {
                        // Current target is no longer a log
                        BlockPos nextLog = findIntelligentTarget(world, player.getBlockPos(), state.targetTree, state);
                        if (nextLog != null) {
                            state.targetTree = nextLog;
                            startLookingAt(player, state, nextLog);
                            state.ticksSinceLastAction = 0;
                            intelligence.recordProgress("Found next log");
                            ChipperChopperMod.LOGGER.info("Current log destroyed, intelligent system found: " + nextLog);
                        } else {
                            ChipperChopperMod.LOGGER.info("Agent.Lumber: Tree completed successfully");
                            state.currentTask = AIState.Task.COLLECTING;
                            state.targetTree = null;
                            state.thinkingState = AIThinkingState.COLLECTING;
                            intelligence.recordSuccess("Tree completed");
                        }
                    }
                } else if (state.targetTree == null) {
                    state.currentTask = AIState.Task.IDLE;
                    state.thinkingState = AIThinkingState.IDLE;
                }
                
                // Much faster timeout with smarter recovery
                if (state.ticksSinceLastAction > 30) { // Reduced from 40 to 30
                    intelligence.recordFailure(state.targetTree, "Chopping timeout");
                    
                    // Don't try recovery if we've been cycling - just abandon
                    if (intelligence.isInIntelligentUpgradeLoop(state.targetTree) || 
                        intelligence.hasRecentlyRepositioned(state.targetTree)) {
                        ChipperChopperMod.LOGGER.info("Agent.Lumber: Timeout with recent cycling - immediate abandonment");
                        intelligence.blacklistTarget(state.targetTree, "Timeout after cycling");
                        state.currentTask = AIState.Task.COLLECTING;
                        state.targetTree = null;
                    } else {
                        // Try one recovery attempt
                        BlockPos recoveryTarget = findIntelligentTarget(world, player.getBlockPos(), state.targetTree, state);
                        if (recoveryTarget != null && !recoveryTarget.equals(state.targetTree) &&
                            hasServerLineOfSight(world, player.getBlockPos(), recoveryTarget)) {
                            state.targetTree = recoveryTarget;
                            startLookingAt(player, state, recoveryTarget);
                            state.ticksSinceLastAction = 0;
                            state.consecutiveLineOfSightFailures = 0;
                            intelligence.recordProgress("Timeout recovery");
                            ChipperChopperMod.LOGGER.info("Agent.Lumber: Single timeout recovery attempt: " + recoveryTarget);
                        } else {
                            intelligence.blacklistTarget(state.targetTree, "Timeout - no valid recovery");
                            state.currentTask = AIState.Task.COLLECTING;
                            state.targetTree = null;
                            ChipperChopperMod.LOGGER.info("Agent.Lumber: Timeout - no recovery possible");
                        }
                    }
                }
                break;
                
            case REPOSITIONING:
                if (state.repositionTarget != null) {
                    double distance = player.getPos().distanceTo(Vec3d.ofCenter(state.repositionTarget));
                    
                    if (distance <= 1.5) {
                        // Reached reposition target, determine next action
                        if (state.currentLeafTarget != null) {
                            // Were repositioning for leaf clearing
                            state.currentTask = AIState.Task.CLEARING_LEAVES;
                        } else {
                            // Were repositioning for tree chopping
                            state.currentTask = AIState.Task.CHOPPING;
                        }
                        state.ticksSinceLastAction = 0;
                        state.consecutiveLineOfSightFailures = 0; // Reset LOS failures
                        state.repositionTarget = null;
                        ChipperChopperMod.LOGGER.info("Repositioning complete, resuming task");
                        if (state.targetTree != null) {
                            startLookingAt(player, state, state.targetTree);
                        }
                    } else if (state.ticksSinceLastAction > 200) { // 10 seconds timeout
                        // Repositioning taking too long, give up
                        state.currentTask = AIState.Task.IDLE;
                        state.repositionTarget = null;
                        state.targetTree = null;
                        state.currentLeafTarget = null;
                        ChipperChopperMod.LOGGER.info("Repositioning timeout, giving up");
                    } else {
                        // Client handles movement to reposition target
                        state.isMoving = true;
                    }
                } else {
                    // No reposition target, return to previous task
                    if (state.currentLeafTarget != null) {
                        state.currentTask = AIState.Task.CLEARING_LEAVES;
                    } else {
                        state.currentTask = AIState.Task.CHOPPING;
                    }
                }
                break;
                
            case COLLECTING:
                // Enhanced item collection with actual movement
                if (collectNearbyItems(player, state)) {
                    state.ticksSinceLastAction = 0;
                } else {
                    // No items found, return to idle after a short delay
                    if (state.ticksSinceLastAction > 60) { // 3 seconds delay
                        state.currentTask = AIState.Task.IDLE;
                        state.collectionAttempts = 0; // Reset collection attempts
                        ChipperChopperMod.LOGGER.info("No more items to collect, returning to idle");
                    }
                }
                break;
                
            case MOVING_TO_ITEM:
                if (state.targetItem != null) {
                    double distance = player.getPos().distanceTo(Vec3d.ofCenter(state.targetItem));
                    
                    if (distance <= 2.0) {
                        // Close enough to collect item
                        state.currentTask = AIState.Task.COLLECTING;
                        state.targetItem = null;
                        state.isMoving = false;
                        ChipperChopperMod.LOGGER.info("Reached item, collecting");
                    } else if (distance > 20) {
                        // Item too far or might have despawned
                        state.currentTask = AIState.Task.COLLECTING;
                        state.targetItem = null;
                        ChipperChopperMod.LOGGER.info("Item too far, searching for others");
                    } else {
                        // Continue moving toward item
                        state.isMoving = true;
                    }
                } else {
                    state.currentTask = AIState.Task.COLLECTING;
                }
                break;
        }
        
        // Always update rotation if we have a target
        updateRotation(player, state);
    }
    
    private static void startLookingAt(ServerPlayerEntity player, AIState state, BlockPos target) {
        Vec3d playerPos = player.getPos().add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3d targetPos = Vec3d.ofCenter(target);
        Vec3d direction = targetPos.subtract(playerPos);
        
        // Calculate target yaw (horizontal rotation)
        double deltaX = direction.x;
        double deltaZ = direction.z;
        float targetYaw = (float) (Math.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0f;
        
        // Calculate target pitch (vertical rotation)
        double deltaY = direction.y;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float targetPitch = (float) -(Math.atan2(deltaY, horizontalDistance) * 180.0 / Math.PI);
        
        // Clamp pitch to reasonable values
        targetPitch = Math.max(-90.0f, Math.min(90.0f, targetPitch));
        
        state.targetRotation = new Vec3d(targetYaw, targetPitch, 0);
        
        ChipperChopperMod.LOGGER.info("Starting to look at " + target + 
            " (target yaw: " + String.format("%.1f", targetYaw) + 
            ", target pitch: " + String.format("%.1f", targetPitch) + ")");
    }
    
    private static void updateRotation(ServerPlayerEntity player, AIState state) {
        if (state.targetRotation == null) return;
        
        float currentYaw = player.getYaw();
        float currentPitch = player.getPitch();
        float targetYaw = (float) state.targetRotation.x;
        float targetPitch = (float) state.targetRotation.y;
        
        // Calculate shortest angular distance for yaw (handle wrap-around)
        float yawDiff = targetYaw - currentYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        
        float pitchDiff = targetPitch - currentPitch;
        
        // Apply smooth rotation
        float yawStep = Math.signum(yawDiff) * Math.min(Math.abs(yawDiff), (float) ROTATION_SPEED);
        float pitchStep = Math.signum(pitchDiff) * Math.min(Math.abs(pitchDiff), (float) ROTATION_SPEED);
        
        float newYaw = currentYaw + yawStep;
        float newPitch = MathHelper.clamp(currentPitch + pitchStep, -90.0f, 90.0f);
        
        // Apply the rotation directly - this should sync to client automatically
        player.setYaw(newYaw);
        player.setPitch(newPitch);
        
        // Check if we've reached the target rotation
        if (Math.abs(yawDiff) < 2.0f && Math.abs(pitchDiff) < 2.0f) {
            state.targetRotation = null; // Stop rotating
        }
    }
    
    private static boolean isLookingAtTarget(ServerPlayerEntity player, BlockPos target) {
        Vec3d playerPos = player.getPos().add(0, player.getEyeHeight(player.getPose()), 0);
        Vec3d targetPos = Vec3d.ofCenter(target);
        Vec3d direction = targetPos.subtract(playerPos).normalize();
        
        // Get player's look direction
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        Vec3d lookDirection = Vec3d.fromPolar(pitch, yaw);
        
        // Check if directions are close enough (within 5 degrees)
        double dotProduct = lookDirection.dotProduct(direction);
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, dotProduct))) * 180.0 / Math.PI;
        
        return angle < 5.0; // Within 5 degrees
    }
    
    // Server-side movement removed - client now handles all movement via key presses
    
    private static void findNearestTree(ServerPlayerEntity player, AIState state) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        BlockPos nearestTree = null;
        double nearestDistance = Double.MAX_VALUE;
        
        ChipperChopperMod.LOGGER.info("Searching for trees around player at: " + playerPos);
        
        // Search for trees in a radius around the player
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x += 2) { // Skip every other block for performance
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z += 2) {
                for (int y = -5; y <= 10; y++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    BlockState blockState = world.getBlockState(pos);
                    
                    if (isLogBlock(blockState)) {
                        // Check if this is a tree base (has ground below)
                        if (isTreeBase(world, pos)) {
                            double distance = player.getPos().distanceTo(Vec3d.ofCenter(pos));
                            if (distance < nearestDistance && distance > 2.0) { // Not too close
                                nearestDistance = distance;
                                nearestTree = pos;
                            }
                        }
                    }
                }
            }
        }
        
        if (nearestTree != null) {
            state.targetTree = nearestTree;
            state.currentTask = AIState.Task.MOVING_TO_TREE;
            state.ticksSinceLastAction = 0;
            ChipperChopperMod.LOGGER.info("Found tree at: " + nearestTree + " (distance: " + String.format("%.2f", nearestDistance) + ")");
        } else {
            ChipperChopperMod.LOGGER.info("No trees found in search radius");
        }
    }
    
    private static boolean isTreeBase(World world, BlockPos logPos) {
        // Check if there's ground below this log
        for (int i = 1; i <= 3; i++) {
            BlockPos below = logPos.down(i);
            BlockState belowState = world.getBlockState(below);
            
            if (belowState.isOf(Blocks.DIRT) || belowState.isOf(Blocks.GRASS_BLOCK) || 
                belowState.isOf(Blocks.PODZOL) || belowState.isOf(Blocks.COARSE_DIRT) ||
                belowState.isOf(Blocks.ROOTED_DIRT) || belowState.isOf(Blocks.MYCELIUM)) {
                return true;
            }
            
            // If we hit another log, keep checking
            if (!isLogBlock(belowState)) {
                break;
            }
        }
        
        // For 2x2 trees, check if this is part of a 2x2 log pattern at ground level
        if (world.getBlockState(logPos.down()).isAir() || 
            world.getBlockState(logPos.down()).isOf(Blocks.SHORT_GRASS) ||
            world.getBlockState(logPos.down()).isOf(Blocks.TALL_GRASS)) {
            
            // Check for adjacent logs at the same level (indicating 2x2 base)
            BlockPos[] adjacent = {
                logPos.north(), logPos.south(), logPos.east(), logPos.west(),
                logPos.north().east(), logPos.north().west(), 
                logPos.south().east(), logPos.south().west()
            };
            
            int adjacentLogs = 0;
            for (BlockPos adj : adjacent) {
                if (isLogBlock(world.getBlockState(adj))) {
                    adjacentLogs++;
                }
            }
            
            // If we have multiple adjacent logs, this is likely a 2x2 tree base
            if (adjacentLogs >= 2) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean collectNearbyItems(ServerPlayerEntity player, AIState state) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        // Look for items within collection radius
        List<ItemEntity> nearbyItems = world.getEntitiesByClass(
            ItemEntity.class,
            new Box(playerPos).expand(COLLECTION_RADIUS),
            item -> item.isAlive() && !item.cannotPickup()
        );
        
        if (!nearbyItems.isEmpty()) {
            // Find closest item
            ItemEntity closestItem = null;
            double closestDistance = Double.MAX_VALUE;
            
            for (ItemEntity item : nearbyItems) {
                double distance = player.getPos().distanceTo(item.getPos());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestItem = item;
                }
            }
            
            if (closestItem != null) {
                BlockPos itemPos = closestItem.getBlockPos();
                double distance = player.getPos().distanceTo(Vec3d.ofCenter(itemPos));
                
                if (distance <= 2.0) {
                    // Close enough - items should auto-collect
                    // Only log every 2 seconds to reduce spam
                    if (state.ticksSinceLastAction % 40 == 0) {
                        ChipperChopperMod.LOGGER.info("Found item to collect nearby");
                    }
                    return true;
                } else {
                    // Need to move to the item - but only if we're not already moving to it
                    if (state.targetItem == null || !state.targetItem.equals(itemPos)) {
                        state.currentTask = AIState.Task.MOVING_TO_ITEM;
                        state.targetItem = itemPos;
                        state.isMoving = true;
                        state.ticksSinceLastAction = 0; // Reset timer for movement
                        ChipperChopperMod.LOGGER.info("Moving towards item at: " + itemPos);
                    }
                    return true;
                }
            }
        }
        
        return false;
    }
    
    // Old server-side mining method removed - client now handles all mining
    
    /**
     * Find the next log block to chop - improved algorithm for better tree traversal
     */
    private static BlockPos findNextLogBlock(World world, BlockPos currentPos) {
        if (currentPos == null) return null;
        
        // Get potential log blocks in order of preference
        List<BlockPos> candidates = new ArrayList<>();
        
        // Priority 1: Check directly above (most efficient for trunk)
        for (int y = 1; y <= 4; y++) { // Check multiple levels up
            BlockPos above = currentPos.up(y);
            if (isLogBlock(world.getBlockState(above))) {
                candidates.add(above);
            }
        }
        
        // Priority 2: Check same level adjacent (for 2x2 trees)
        BlockPos[] adjacentOffsets = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 1), new BlockPos(-1, 0, -1),
            new BlockPos(1, 0, -1), new BlockPos(-1, 0, 1)
        };
        
        for (BlockPos offset : adjacentOffsets) {
            BlockPos checkPos = currentPos.add(offset);
            if (isLogBlock(world.getBlockState(checkPos))) {
                candidates.add(checkPos);
            }
        }
        
        // Priority 3: Check diagonal and elevated positions for 2x2 trees
        for (int y = 1; y <= 3; y++) {
            for (BlockPos offset : adjacentOffsets) {
                BlockPos checkPos = currentPos.up(y).add(offset);
                if (isLogBlock(world.getBlockState(checkPos))) {
                    candidates.add(checkPos);
                }
            }
        }
        
        // Priority 4: Search in a broader radius for scattered logs (branches, etc.)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -1; y <= 3; y++) {
                    BlockPos checkPos = currentPos.add(x, y, z);
                    if (isLogBlock(world.getBlockState(checkPos)) && !candidates.contains(checkPos)) {
                        candidates.add(checkPos);
                    }
                }
            }
        }
        
        // If no candidates found, return null
        if (candidates.isEmpty()) {
            ChipperChopperMod.LOGGER.info("No log blocks found near " + currentPos);
            return null;
        }
        
        ChipperChopperMod.LOGGER.info("Found " + candidates.size() + " potential log blocks");
        
        // Smart selection: prefer blocks that don't require heavy leaf clearing
        for (BlockPos candidate : candidates) {
            int leafCount = countObstructingLeaves(world, currentPos, candidate);
            ChipperChopperMod.LOGGER.info("Candidate " + candidate + " has " + leafCount + " obstructing leaves");
            
            // If this candidate has minimal leaf obstruction, choose it
            if (leafCount <= 2) {
                ChipperChopperMod.LOGGER.info("Selected easily accessible log: " + candidate);
                return candidate;
            }
        }
        
        // If all candidates require leaf clearing, choose the closest one
        BlockPos closest = candidates.get(0);
        double closestDistance = currentPos.getSquaredDistance(closest);
        for (BlockPos candidate : candidates) {
            double distance = currentPos.getSquaredDistance(candidate);
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        
        ChipperChopperMod.LOGGER.info("Selected closest log (may need leaf clearing): " + closest);
        return closest;
    }
    
    /**
     * Count how many leaves are obstructing the path to a target block
     */
    private static int countObstructingLeaves(World world, BlockPos fromPos, BlockPos toPos) {
        Vec3d start = Vec3d.ofCenter(fromPos.up());
        Vec3d end = Vec3d.ofCenter(toPos);
        Vec3d direction = end.subtract(start).normalize();
        double totalDistance = start.distanceTo(end);
        
        int leafCount = 0;
        for (double d = 1.0; d < totalDistance - 0.5; d += 0.5) {
            Vec3d point = start.add(direction.multiply(d));
            BlockPos checkPos = BlockPos.ofFloored(point);
            BlockState state = world.getBlockState(checkPos);
            
            if (isLeafBlock(state)) {
                leafCount++;
            }
        }
        
        return leafCount;
    }
    
    private static boolean isLogBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.OAK_LOG || block == Blocks.BIRCH_LOG || block == Blocks.SPRUCE_LOG ||
               block == Blocks.JUNGLE_LOG || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG ||
               block == Blocks.MANGROVE_LOG || block == Blocks.CHERRY_LOG || block == Blocks.BAMBOO_BLOCK ||
               block == Blocks.CRIMSON_STEM || block == Blocks.WARPED_STEM ||
               // Include stripped variants
               block == Blocks.STRIPPED_OAK_LOG || block == Blocks.STRIPPED_BIRCH_LOG || 
               block == Blocks.STRIPPED_SPRUCE_LOG || block == Blocks.STRIPPED_JUNGLE_LOG ||
               block == Blocks.STRIPPED_ACACIA_LOG || block == Blocks.STRIPPED_DARK_OAK_LOG ||
               block == Blocks.STRIPPED_MANGROVE_LOG || block == Blocks.STRIPPED_CHERRY_LOG ||
               block == Blocks.STRIPPED_CRIMSON_STEM || block == Blocks.STRIPPED_WARPED_STEM;
    }
    
    // === NEW: Helper for client-side movement ===
    /**
     * Get the active target position for client movement
     */
    public static Vec3d getActiveTarget(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        if (state != null) {
            // Priority: repositioning target > tree target > item target
            if (state.currentTask == AIState.Task.REPOSITIONING && state.repositionTarget != null) {
                return Vec3d.ofCenter(state.repositionTarget);
            } else if (state.targetTree != null && 
                      (state.currentTask == AIState.Task.MOVING_TO_TREE || 
                       state.currentTask == AIState.Task.CHOPPING ||
                       state.currentTask == AIState.Task.CLEARING_LEAVES)) {
                return Vec3d.ofCenter(state.targetTree);
            } else if (state.targetItem != null && state.currentTask == AIState.Task.MOVING_TO_ITEM) {
                return Vec3d.ofCenter(state.targetItem);
            }
        }
        return null;
    }
    
    /**
     * Returns the current block that should be mined, or {@code null} if none.
     * This is used by the client-side code to know what block to mine.
     */
    public static BlockPos getCurrentMiningTarget(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        if (state != null) {
            // Return leaf target if we're clearing leaves, otherwise return tree target
            if (state.currentTask == AIState.Task.CLEARING_LEAVES && state.currentLeafTarget != null) {
                return state.currentLeafTarget;
            }
            return state.targetTree;
        }
        return null;
    }
    
    /**
     * Forces the AI to find a new target. Called by client when current target is not accessible.
     */
    public static void forceNewTarget(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        if (state != null && state.currentTask == AIState.Task.CHOPPING) {
            // On client side, we can't access the server world directly
            // So we'll just mark the current target as invalid and let the server-side tick handle it
            if (!(player instanceof ServerPlayerEntity)) {
                // Client-side: just mark target as needing refresh
                state.targetTree = null;
                state.currentTask = AIState.Task.IDLE;
                ChipperChopperMod.LOGGER.info("Client marked target as invalid, server will find new target");
                return;
            }
            
            // Server-side: can do full target finding
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            World world = serverPlayer.getWorld();
            BlockPos nextLog = findNextLogBlock(world, state.targetTree);
            if (nextLog != null) {
                state.targetTree = nextLog;
                startLookingAt(serverPlayer, state, nextLog);
                ChipperChopperMod.LOGGER.info("Found new accessible log block: " + nextLog);
            } else {
                // No more logs, go to collecting
                state.currentTask = AIState.Task.COLLECTING;
                state.targetTree = null;
                state.collectionAttempts = 0;
                ChipperChopperMod.LOGGER.info("No more accessible logs, switching to collecting");
            }
        }
    }
    
    /**
     * Check if the player should currently be mining (used by client)
     */
    public static boolean shouldBeMining(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        if (state == null) return false;
        
        // Should be mining if we're chopping trees or clearing leaves
        return (state.currentTask == AIState.Task.CHOPPING && state.targetTree != null) ||
               (state.currentTask == AIState.Task.CLEARING_LEAVES && state.currentLeafTarget != null);
    }
    
    /**
     * Called by client when mining progress is made - resets timeout
     */
    public static void notifyMiningProgress(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        if (state != null && (state.currentTask == AIState.Task.CHOPPING || state.currentTask == AIState.Task.CLEARING_LEAVES)) {
            state.ticksSinceLastAction = Math.max(0, state.ticksSinceLastAction - 10); // Reduce timeout when making progress
            state.consecutiveLineOfSightFailures = 0; // Reset LOS failures
        }
    }
    
    /**
     * Called by client when line of sight to target is lost - helps server adapt
     */
    public static void notifyLineOfSightFailure(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        if (state != null) {
            state.consecutiveLineOfSightFailures++;
            
            // Only log periodically to reduce spam
            if (state.consecutiveLineOfSightFailures % 20 == 1) { // Log every 20 failures (once per second)
                ChipperChopperMod.LOGGER.info("Line of sight failure #" + state.consecutiveLineOfSightFailures + 
                    " for target " + (state.targetTree != null ? state.targetTree : state.currentLeafTarget));
            }
            
            // After multiple failures, take corrective action
            if (state.consecutiveLineOfSightFailures >= 40) { // 2 seconds of failures
                if (!(player instanceof ServerPlayerEntity)) {
                    return; // Client can't make server-side decisions
                }
                
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                World world = serverPlayer.getWorld();
                
                if (state.currentTask == AIState.Task.CHOPPING && state.targetTree != null) {
                    ChipperChopperMod.LOGGER.info("Persistent line-of-sight failure, finding alternative approach");
                    
                    // Try to find a more accessible log block
                    BlockPos alternativeLog = findAlternativeLogBlock(world, player.getBlockPos(), state.targetTree);
                    if (alternativeLog != null && !alternativeLog.equals(state.targetTree)) {
                        state.targetTree = alternativeLog;
                        startLookingAt(serverPlayer, state, alternativeLog);
                        state.consecutiveLineOfSightFailures = 0;
                        state.ticksSinceLastAction = 0;
                        ChipperChopperMod.LOGGER.info("Switched to alternative log due to LOS failures: " + alternativeLog);
                        return;
                    }
                    
                    // If no alternative log, try repositioning
                    BlockPos betterPos = findBetterPosition(world, player.getBlockPos(), state.targetTree);
                    if (betterPos != null) {
                        state.currentTask = AIState.Task.REPOSITIONING;
                        state.repositionTarget = betterPos;
                        state.consecutiveLineOfSightFailures = 0;
                        ChipperChopperMod.LOGGER.info("Repositioning due to persistent LOS failures: " + betterPos);
                        return;
                    }
                    
                    // If target might be behind leaves, switch to leaf clearing
                    if (shouldClearLeavesFirst(world, player.getBlockPos(), state.targetTree)) {
                        state.currentTask = AIState.Task.CLEARING_LEAVES;
                        state.currentLeafTarget = null;
                        state.consecutiveLineOfSightFailures = 0;
                        ChipperChopperMod.LOGGER.info("Switching to leaf clearing due to LOS failures");
                        return;
                    }
                    
                    // Last resort: find a new tree
                    state.currentTask = AIState.Task.IDLE;
                    state.targetTree = null;
                    state.consecutiveLineOfSightFailures = 0;
                    ChipperChopperMod.LOGGER.info("Giving up on inaccessible tree, searching for new one");
                    
                } else if (state.currentTask == AIState.Task.CLEARING_LEAVES && state.currentLeafTarget != null) {
                    ChipperChopperMod.LOGGER.info("Line-of-sight failure during leaf clearing, finding alternative");
                    
                    // Try to find a different leaf to clear
                    BlockPos alternativeLeaf = findAlternativeLeafTarget(world, player.getBlockPos(), state.targetTree, state.problematicLeaves);
                    if (alternativeLeaf != null && !alternativeLeaf.equals(state.currentLeafTarget)) {
                        state.currentLeafTarget = alternativeLeaf;
                        startLookingAt(serverPlayer, state, alternativeLeaf);
                        state.consecutiveLineOfSightFailures = 0;
                        state.ticksSinceLastAction = 0;
                        ChipperChopperMod.LOGGER.info("Switched to alternative leaf: " + alternativeLeaf);
                        return;
                    }
                    
                    // If no alternative leaf, try repositioning
                    BlockPos betterPos = findBetterLeafClearingPosition(world, player.getBlockPos(), state.targetTree);
                    if (betterPos != null) {
                        state.currentTask = AIState.Task.REPOSITIONING;
                        state.repositionTarget = betterPos;
                        state.consecutiveLineOfSightFailures = 0;
                        ChipperChopperMod.LOGGER.info("Repositioning for better leaf access");
                        return;
                    }
                    
                    // Give up on leaf clearing, go back to chopping or idle
                    state.currentTask = AIState.Task.CHOPPING;
                    state.currentLeafTarget = null;
                    state.consecutiveLineOfSightFailures = 0;
                    ChipperChopperMod.LOGGER.info("Abandoning problematic leaves, resuming chopping");
                }
            }
            
            // If repositioning is not possible or has failed, give up on this tree
            if (state.consecutiveLineOfSightFailures >= 12) {
                // No alternatives found, complete this tree and move on
                state.currentTask = AIState.Task.COLLECTING;
                state.targetTree = null;
                state.repositionAttempts = 0;
                state.consecutiveLineOfSightFailures = 0;
                ChipperChopperMod.LOGGER.info("Too many line-of-sight failures, giving up on this tree and moving to collection");
            }
        }
    }
    
    /**
     * Find a LOWER log block that's more likely to be accessible (prioritizes ground-level logs)
     */
    private static BlockPos findLowerLogBlock(World world, BlockPos playerPos, BlockPos currentTarget) {
        int playerY = playerPos.getY();
        BlockPos bestCandidate = null;
        int bestY = Integer.MAX_VALUE;
        
        // Search in a radius around the player for log blocks, prioritizing lower ones
        for (int x = -4; x <= 4; x++) {
            for (int y = -2; y <= 3; y++) { // Don't search too high up
                for (int z = -4; z <= 4; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    
                    // Skip the current target
                    if (checkPos.equals(currentTarget)) continue;
                    
                    if (isLogBlock(world.getBlockState(checkPos))) {
                        // Prioritize blocks closer to player's Y level (easier to reach)
                        int blockY = checkPos.getY();
                        if (blockY <= playerY + 2 && blockY < bestY) { // Prefer blocks at or near player level
                            bestCandidate = checkPos;
                            bestY = blockY;
                        }
                    }
                }
            }
        }
        
        return bestCandidate;
    }

    /**
     * Find an alternative log block near the player when the current target is inaccessible
     */
    private static BlockPos findAlternativeLogBlock(World world, BlockPos playerPos, BlockPos currentTarget) {
        // Search in a small radius around the player for any log blocks
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 5; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    
                    // Skip the current target
                    if (checkPos.equals(currentTarget)) continue;
                    
                    if (isLogBlock(world.getBlockState(checkPos))) {
                        // Check if this block is closer to the player than the current target
                        double distanceToCheck = playerPos.getSquaredDistance(checkPos.getX(), checkPos.getY(), checkPos.getZ());
                        double distanceToCurrent = playerPos.getSquaredDistance(currentTarget.getX(), currentTarget.getY(), currentTarget.getZ());
                        
                        if (distanceToCheck <= distanceToCurrent + 4) { // Within reasonable range
                            return checkPos;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Find a better position for the player to reach the target block
     */
    private static BlockPos findBetterPosition(World world, BlockPos playerPos, BlockPos targetBlock) {
        // Calculate direction from player to target
        Vec3d direction = Vec3d.ofCenter(targetBlock).subtract(Vec3d.ofCenter(playerPos)).normalize();
        
        // Try positions closer to the target in steps
        for (int distance = 1; distance <= 4; distance++) {
            Vec3d newPos = Vec3d.ofCenter(playerPos).add(direction.multiply(distance));
            BlockPos testPos = BlockPos.ofFloored(newPos);
            
            // Check if this position is safe and accessible
            if (isValidPosition(world, testPos)) {
                // Check if we'd have line of sight from this position to target
                if (hasServerLineOfSight(world, testPos, targetBlock)) {
                    return testPos;
                }
            }
        }
        
        // Try positions around the target in a circle
        for (int radius = 2; radius <= 4; radius++) {
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                int offsetX = (int) Math.round(radius * Math.cos(radians));
                int offsetZ = (int) Math.round(radius * Math.sin(radians));
                
                BlockPos testPos = targetBlock.add(offsetX, 0, offsetZ);
                
                if (isValidPosition(world, testPos)) {
                    if (hasServerLineOfSight(world, testPos, targetBlock)) {
                        return testPos;
                    }
                }
            }
        }
        
        return null; // No better position found
    }
    
    /**
     * Check if a position is safe for the player to stand
     */
    private static boolean isValidPosition(World world, BlockPos pos) {
        // Check feet level (must be air or passable)
        BlockState feetBlock = world.getBlockState(pos);
        if (!feetBlock.isAir() && !feetBlock.getCollisionShape(world, pos).isEmpty()) {
            return false;
        }
        
        // Check head level (must be air or passable)
        BlockState headBlock = world.getBlockState(pos.up());
        if (!headBlock.isAir() && !headBlock.getCollisionShape(world, pos.up()).isEmpty()) {
            return false;
        }
        
        // Check ground level (must have solid ground)
        BlockState groundBlock = world.getBlockState(pos.down());
        return !groundBlock.isAir() && !groundBlock.getCollisionShape(world, pos.down()).isEmpty();
    }
    
    /**
     * Server-side line-of-sight check
     */
    private static boolean hasServerLineOfSight(World world, BlockPos fromPos, BlockPos toPos) {
        Vec3d fromCenter = Vec3d.ofCenter(fromPos).add(0, 1.6, 0); // Eye level
        Vec3d toCenter = Vec3d.ofCenter(toPos);
        
        // Simple raycast check - in a real implementation you'd use world.raycast
        Vec3d direction = toCenter.subtract(fromCenter).normalize();
        double distance = fromCenter.distanceTo(toCenter);
        
        // Check every 0.5 blocks along the ray
        for (double d = 0.5; d < distance; d += 0.5) {
            Vec3d checkPos = fromCenter.add(direction.multiply(d));
            BlockPos checkBlock = BlockPos.ofFloored(checkPos);
            
            if (!world.getBlockState(checkBlock).isAir()) {
                // Hit something solid, check if it's our target
                return checkBlock.equals(toPos);
            }
        }
        
        return true; // Clear line of sight
    }
    
    /**
     * Check if the AI is currently repositioning
     */
    public static boolean isRepositioning(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        return state != null && state.currentTask == AIState.Task.REPOSITIONING;
    }
    
    /**
     * Check if the AI is currently clearing leaves
     */
    public static boolean isClearingLeaves(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        return state != null && state.currentTask == AIState.Task.CLEARING_LEAVES;
    }
    
    /**
     * Get the current repositioning target
     */
    public static BlockPos getRepositioningTarget(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        if (state != null && state.currentTask == AIState.Task.REPOSITIONING) {
            return state.repositionTarget;
        }
        return null;
    }
    
    /**
     * Check if we need to clear leaves before chopping the tree
     */
    private static boolean shouldClearLeavesFirst(World world, BlockPos playerPos, BlockPos treePos) {
        // Check if there are leaves blocking direct line of sight
        BlockPos leafPos = findObstructingLeaf(world, playerPos, treePos);
        return leafPos != null;
    }
    
    /**
     * Find a leaf block that's obstructing our view of the tree
     */
    private static BlockPos findObstructingLeaf(World world, BlockPos playerPos, BlockPos treePos) {
        Vec3d start = Vec3d.ofCenter(playerPos.up()); // Player eye level
        Vec3d end = Vec3d.ofCenter(treePos);
        Vec3d direction = end.subtract(start).normalize();
        double totalDistance = start.distanceTo(end);
        
        // Raycast from player to tree, looking for leaves
        for (double d = 1.0; d < totalDistance - 0.5; d += 0.3) { // Start further out and use smaller steps
            Vec3d point = start.add(direction.multiply(d));
            BlockPos checkPos = BlockPos.ofFloored(point);
            BlockState state = world.getBlockState(checkPos);
            
            // Check if this is a leaf block that's blocking our path
            if (isLeafBlock(state)) {
                ChipperChopperMod.LOGGER.info("Found obstructing leaf at: " + checkPos + " (distance: " + String.format("%.1f", d) + ")");
                return checkPos;
            }
            
            // Also check if it's a solid block that's not our target (could be leaves or other obstacle)
            if (!state.isAir() && !checkPos.equals(treePos) && !isLogBlock(state)) {
                if (isLeafBlock(state)) {
                    ChipperChopperMod.LOGGER.info("Found non-log obstruction (leaf) at: " + checkPos);
                    return checkPos;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Check if a block state is a leaf block
     */
    private static boolean isLeafBlock(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.OAK_LEAVES || 
               block == Blocks.BIRCH_LEAVES || 
               block == Blocks.SPRUCE_LEAVES || 
               block == Blocks.JUNGLE_LEAVES || 
               block == Blocks.ACACIA_LEAVES || 
               block == Blocks.DARK_OAK_LEAVES ||
               block == Blocks.MANGROVE_LEAVES ||
               block == Blocks.CHERRY_LEAVES ||
               block == Blocks.PALE_OAK_LEAVES;
    }
    
    /**
     * Find a better position for clearing leaves
     */
    private static BlockPos findBetterLeafClearingPosition(World world, BlockPos playerPos, BlockPos treePos) {
        // Try positions around the tree base
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos testPos = treePos.add(x, 0, z);
                if (isValidPosition(world, testPos)) {
                    // Check if this position has fewer obstructing leaves
                    BlockPos leafObstruct = findObstructingLeaf(world, testPos, treePos);
                    if (leafObstruct == null || testPos.getSquaredDistance(leafObstruct) < playerPos.getSquaredDistance(leafObstruct)) {
                        return testPos;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Find a position underneath the tree for better chopping angle
     */
    private static BlockPos findPositionUnderTree(World world, BlockPos playerPos, BlockPos treePos) {
        // Look for a position directly under or near the tree trunk
        BlockPos basePos = findTreeBase(world, treePos);
        if (basePos == null) {
            basePos = treePos;
        }
        
        // Try the base position and surrounding area
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos testPos = basePos.add(x, 0, z);
                if (isValidPosition(world, testPos) && testPos.getY() <= treePos.getY()) {
                    return testPos;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find the base of the tree (lowest log block)
     */
    private static BlockPos findTreeBase(World world, BlockPos startPos) {
        BlockPos current = startPos;
        
        // Go down until we find non-log or hit ground
        while (current.getY() > 0) {
            BlockPos below = current.down();
            if (!isLogBlock(world.getBlockState(below))) {
                return current; // This is the base
            }
            current = below;
        }
        
        return current;
    }
    
    /**
     * Find an alternative leaf target that might be easier to break
     */
    private static BlockPos findAlternativeLeafTarget(World world, BlockPos playerPos, BlockPos treePos, Set<BlockPos> problematicLeaves) {
        List<BlockPos> leafCandidates = new ArrayList<>();
        
        // Search for leaf blocks in the area around the line of sight
        Vec3d start = Vec3d.ofCenter(playerPos.up());
        Vec3d end = Vec3d.ofCenter(treePos);
        Vec3d direction = end.subtract(start).normalize();
        double totalDistance = start.distanceTo(end);
        
        // Look for leaves along the path and slightly off to the sides
        for (double d = 1.0; d < totalDistance - 0.5; d += 0.5) {
            Vec3d point = start.add(direction.multiply(d));
            
            // Check the main point and adjacent positions
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos checkPos = BlockPos.ofFloored(point).add(x, y, z);
                        BlockState state = world.getBlockState(checkPos);
                        
                        if (isLeafBlock(state) && !problematicLeaves.contains(checkPos)) {
                            leafCandidates.add(checkPos);
                        }
                    }
                }
            }
        }
        
        if (leafCandidates.isEmpty()) {
            return null;
        }
        
        // Find the leaf that's closest to the player (should be easier to break)
        BlockPos closest = leafCandidates.get(0);
        double closestDistance = playerPos.getSquaredDistance(closest);
        
        for (BlockPos candidate : leafCandidates) {
            double distance = playerPos.getSquaredDistance(candidate);
            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        
        return closest;
    }
    
    // === PATHFINDING AND OBSTACLE AVOIDANCE ===
    
    /**
     * Check if the player needs to navigate around obstacles to reach the target
     */
    private static boolean needsPathfinding(World world, BlockPos playerPos, BlockPos targetPos) {
        // Check if there's a significant height difference
        int heightDiff = Math.abs(targetPos.getY() - playerPos.getY());
        if (heightDiff > 3) {
            return true;
        }
        
        // Check if player is stuck in a hole or behind obstacles
        return isStuckInHole(world, playerPos) || hasObstaclesBetween(world, playerPos, targetPos);
    }
    
    /**
     * Check if the player is stuck in a hole
     */
    private static boolean isStuckInHole(World world, BlockPos playerPos) {
        // Check if there are solid blocks above the player (indicating a hole/cave)
        int blockedDirections = 0;
        
        // Check 4 horizontal directions for walls
        BlockPos[] directions = {
            playerPos.north(), playerPos.south(), playerPos.east(), playerPos.west()
        };
        
        for (BlockPos checkPos : directions) {
            if (!isValidPosition(world, checkPos) || !isValidPosition(world, checkPos.up())) {
                blockedDirections++;
            }
        }
        
        // If 3+ directions are blocked, likely in a hole/corner
        return blockedDirections >= 3;
    }
    
    /**
     * Check if there are major obstacles between player and target
     */
    private static boolean hasObstaclesBetween(World world, BlockPos playerPos, BlockPos targetPos) {
        Vec3d start = Vec3d.ofCenter(playerPos);
        Vec3d end = Vec3d.ofCenter(targetPos);
        Vec3d direction = end.subtract(start).normalize();
        double distance = start.distanceTo(end);
        
        // Check every 2 blocks along the path
        for (double d = 2.0; d < distance; d += 2.0) {
            Vec3d point = start.add(direction.multiply(d));
            BlockPos checkPos = BlockPos.ofFloored(point);
            
            // Check if there's a wall or pit in the way
            if (!isValidPosition(world, checkPos) || !isValidPosition(world, checkPos.up())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Find an accessible position to reach the target tree
     */
    private static BlockPos findAccessiblePosition(World world, BlockPos playerPos, BlockPos targetTree) {
        // Try positions around the tree at different heights and distances
        int[] distances = {3, 4, 5, 6}; // Try different distances from tree
        int[] heightOffsets = {0, -1, -2, 1, 2}; // Try different heights
        
        for (int distance : distances) {
            for (int heightOffset : heightOffsets) {
                // Try 8 directions around the tree
                for (int angle = 0; angle < 360; angle += 45) {
                    double rad = Math.toRadians(angle);
                    int x = (int) (targetTree.getX() + distance * Math.cos(rad));
                    int z = (int) (targetTree.getZ() + distance * Math.sin(rad));
                    int y = targetTree.getY() + heightOffset;
                    
                    BlockPos candidate = new BlockPos(x, y, z);
                    
                    // Check if this position is accessible and has line of sight to tree
                    if (isValidPosition(world, candidate) && 
                        hasServerLineOfSight(world, candidate, targetTree) &&
                        !needsPathfinding(world, playerPos, candidate)) {
                        
                        // Make sure there's a path from player to this position
                        if (!hasObstaclesBetween(world, playerPos, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Find a position to help the player get out of a hole or stuck situation
     */
    private static BlockPos findEscapePosition(World world, BlockPos playerPos) {
        // Try to find higher ground nearby
        int[] distances = {2, 3, 4, 5};
        
        for (int distance : distances) {
            for (int angle = 0; angle < 360; angle += 30) {
                double rad = Math.toRadians(angle);
                int x = (int) (playerPos.getX() + distance * Math.cos(rad));
                int z = (int) (playerPos.getZ() + distance * Math.sin(rad));
                
                // Try positions 1-3 blocks higher than current
                for (int heightOffset = 1; heightOffset <= 3; heightOffset++) {
                    int y = playerPos.getY() + heightOffset;
                    BlockPos candidate = new BlockPos(x, y, z);
                    
                    if (isValidPosition(world, candidate) && !isStuckInHole(world, candidate)) {
                        return candidate;
                    }
                }
            }
        }
        
        return null;
    }
    
    // === ENHANCED AI INTELLIGENCE SYSTEM ===
    
    /**
     * Enhanced AI decision tracking and progress monitoring
     */
    private static class AIDecisionTracker {
        public Set<BlockPos> attemptedTargets = new HashSet<>();
        public Map<BlockPos, Integer> targetAttemptCounts = new HashMap<>();
        public long lastProgressTime = System.currentTimeMillis();
        public int consecutiveFailures = 0;
        public String currentStrategy = "INITIAL";
        public List<String> decisionHistory = new ArrayList<>();
        
        public void recordAttempt(BlockPos target, String reason) {
            attemptedTargets.add(target);
            targetAttemptCounts.put(target, targetAttemptCounts.getOrDefault(target, 0) + 1);
            decisionHistory.add(System.currentTimeMillis() + ": " + reason + " -> " + target);
            if (decisionHistory.size() > 10) {
                decisionHistory.remove(0); // Keep only last 10 decisions
            }
        }
        
        public boolean hasTriedTarget(BlockPos target) {
            return targetAttemptCounts.getOrDefault(target, 0) >= 2;
        }
        
        public void recordProgress() {
            lastProgressTime = System.currentTimeMillis();
            consecutiveFailures = 0;
        }
        
        public void recordFailure() {
            consecutiveFailures++;
        }
        
        public boolean shouldAbandonCurrentApproach() {
            long timeSinceProgress = System.currentTimeMillis() - lastProgressTime;
            return timeSinceProgress > 15000 || consecutiveFailures > 5; // 15 seconds or 5 failures
        }
        
        public void reset() {
            attemptedTargets.clear();
            targetAttemptCounts.clear();
            lastProgressTime = System.currentTimeMillis();
            consecutiveFailures = 0;
            decisionHistory.clear();
        }
    }
    
    /**
     * AI Thinking states for HUD display
     */
    public enum AIThinkingState {
        SCANNING("🔍 Scanning for trees..."),
        PATHFINDING("🧭 Planning route to tree..."),
        ANALYZING("🧠 Analyzing tree structure..."),
        CHOPPING("🪓 Chopping wood blocks..."),
        CLEARING_OBSTACLES("🌿 Clearing leaf obstacles..."),
        REPOSITIONING("🏃 Repositioning for better access..."),
        COLLECTING("📦 Collecting dropped items..."),
        PROBLEM_SOLVING("🤔 Finding alternative approach..."),
        STUCK_RECOVERY("⚡ Recovering from stuck state..."),
        IDLE("😴 Waiting for activation...");
        
        private final String description;
        
        AIThinkingState(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Enhanced progress tracking for better decision making
     */
    private static void trackProgress(AIState state, String action) {
        if (state.decisionTracker == null) {
            state.decisionTracker = new AIDecisionTracker();
        }
        
        state.decisionTracker.recordProgress();
        ChipperChopperMod.LOGGER.info("AI Progress: " + action);
    }
    
    /**
     * Intelligent target selection with learning from failures
     */
    private static BlockPos findIntelligentTarget(World world, BlockPos playerPos, BlockPos currentTree, AIState state) {
        if (state.decisionTracker == null) {
            state.decisionTracker = new AIDecisionTracker();
        }
        
        // Get all potential log blocks
        List<BlockPos> candidates = new ArrayList<>();
        
        // Search in expanding radius
        for (int radius = 1; radius <= 4; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -2; y <= 4; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos candidate = currentTree.add(x, y, z);
                        if (isLogBlock(world.getBlockState(candidate)) && 
                            !state.decisionTracker.hasTriedTarget(candidate)) {
                            candidates.add(candidate);
                        }
                    }
                }
            }
            
            if (!candidates.isEmpty()) break; // Found candidates at this radius
        }
        
        if (candidates.isEmpty()) {
            state.thinkingState = AIThinkingState.PROBLEM_SOLVING;
            return null;
        }
        
        // Intelligent scoring system
        BlockPos bestCandidate = null;
        double bestScore = -1;
        
        for (BlockPos candidate : candidates) {
            double score = calculateTargetScore(world, playerPos, candidate, state);
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidate;
            }
        }
        
        if (bestCandidate != null) {
            state.decisionTracker.recordAttempt(bestCandidate, "Intelligent target selection");
            state.thinkingState = AIThinkingState.ANALYZING;
        }
        
        return bestCandidate;
    }
    
    /**
     * Advanced scoring system for target selection
     */
    private static double calculateTargetScore(World world, BlockPos playerPos, BlockPos target, AIState state) {
        double score = 100.0; // Base score
        
        // Distance penalty (closer is better)
        double distance = playerPos.getSquaredDistance(target);
        score -= distance * 2;
        
        // Height accessibility (prefer reachable heights)
        int heightDiff = Math.abs(target.getY() - playerPos.getY());
        if (heightDiff > 3) score -= heightDiff * 10;
        
        // Leaf obstruction penalty
        int leafCount = countObstructingLeaves(world, playerPos, target);
        score -= leafCount * 15;
        
        // Line of sight bonus
        if (hasServerLineOfSight(world, playerPos, target)) {
            score += 30;
        }
        
        // Terrain accessibility
        if (needsPathfinding(world, playerPos, target)) {
            score -= 25;
        }
        
        // Avoid recently failed targets
        if (state.decisionTracker != null) {
            int attempts = state.decisionTracker.targetAttemptCounts.getOrDefault(target, 0);
            score -= attempts * 40; // Heavy penalty for repeated attempts
        }
        
        return score;
    }
    
    private static class AIState {
        public enum Task {
            IDLE, MOVING_TO_TREE, CHOPPING, COLLECTING, MOVING_TO_ITEM, REPOSITIONING, CLEARING_LEAVES
        }
        
        public Task currentTask = Task.IDLE;
        public BlockPos targetTree = null;
        public BlockPos targetItem = null;
        public BlockPos repositionTarget = null; // New field for repositioning
        public BlockPos currentLeafTarget = null; // New field for leaf clearing
        public int ticksSinceLastAction = 0;
        public int chopCooldown = 0;
        public int collectionAttempts = 0; // Track collection attempts to prevent getting stuck
        public boolean isMoving = false;
        public Vec3d targetRotation = null;
        public int repositionAttempts = 0; // Track repositioning attempts
        public int consecutiveLineOfSightFailures = 0; // Track how many ticks client can't see target
        public Set<BlockPos> problematicLeaves = new HashSet<>(); // Track leaves that cause problems
        public int leafClearingFailures = 0; // Track consecutive leaf clearing failures
        public long lastLeafTargetTime = 0; // Track when we last changed leaf targets
        public AIThinkingState thinkingState = AIThinkingState.IDLE;
        public AIDecisionTracker decisionTracker = null;
    }
    
    /**
     * Get current AI thinking state for HUD display
     */
    public static AIThinkingState getCurrentThinkingState(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        return state != null ? state.thinkingState : AIThinkingState.IDLE;
    }
    
    /**
     * Get recent decision history for HUD display
     */
    public static List<String> getRecentDecisions(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        if (state != null && state.decisionTracker != null) {
            return new ArrayList<>(state.decisionTracker.decisionHistory);
        }
        return new ArrayList<>();
    }
    
    /**
     * Get AI statistics for HUD display
     */
    public static String getAIStats(PlayerEntity player) {
        AIState state = playerStates.get(player.getUuid());
        AdvancedIntelligence intelligence = playerIntelligence.get(player.getUuid());
        if (state != null && intelligence != null) {
            return String.format("Failures: %d | Blacklisted: %d | Successes: %d", 
                intelligence.failureCount,
                intelligence.blacklistedTargets.size(),
                intelligence.successCount);
        } else if (state != null && state.decisionTracker != null) {
            long timeSinceProgress = System.currentTimeMillis() - state.decisionTracker.lastProgressTime;
            return String.format("Failures: %d | Progress: %.1fs ago", 
                state.decisionTracker.consecutiveFailures, 
                timeSinceProgress / 1000.0);
        }
        return "No data available";
    }
    
    // NEW: Advanced intelligence methods
    private static void emergencyReset(AIState state, AdvancedIntelligence intelligence) {
        ChipperChopperMod.LOGGER.info("Agent.Lumber: EMERGENCY RESET - Clearing all state and intelligence data");
        
        // Reset AI state
        state.currentTask = AIState.Task.IDLE;
        state.targetTree = null;
        state.targetItem = null;
        state.repositionTarget = null;
        state.currentLeafTarget = null;
        state.ticksSinceLastAction = 0;
        state.consecutiveLineOfSightFailures = 0;
        state.thinkingState = AIThinkingState.IDLE;
        
        // Clear intelligence data including new pattern detection
        intelligence.clearBlacklist();
        intelligence.positionHistory.clear();
        intelligence.leafClearingAttempts.clear();
        intelligence.intelligentUpgradeHistory.clear();
        intelligence.leafClearingFailures.clear();
        intelligence.loopingTargets.clear();
        intelligence.forcedExplorationMode = true; // Force exploration after reset
        intelligence.failureCount = 0;
        intelligence.successCount = 0;
        
        ChipperChopperMod.LOGGER.info("Agent.Lumber: Emergency reset complete, forcing exploration mode");
    }
    
    private static boolean findNearestTreeIntelligent(ServerPlayerEntity player, AIState state, AdvancedIntelligence intelligence) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        ChipperChopperMod.LOGGER.info("Searching for trees around player at: " + playerPos);
        
        // NEW: If in forced exploration mode, expand search radius significantly
        int searchRadius = intelligence.forcedExplorationMode ? FORCED_EXPLORATION_RADIUS : SEARCH_RADIUS;
        if (intelligence.forcedExplorationMode) {
            ChipperChopperMod.LOGGER.info("Agent.Lumber: Forced exploration mode - expanded search radius to " + searchRadius);
        }
        
        BlockPos bestTree = null;
        double bestScore = -1;
        List<BlockPos> candidateTrees = new ArrayList<>();
        
        // Collect all potential trees first
        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int z = -searchRadius; z <= searchRadius; z++) {
                for (int y = -3; y <= 6; y++) { // Expanded vertical search
                    BlockPos logPos = playerPos.add(x, y, z);
                    
                    if (isLogBlock(world.getBlockState(logPos)) && 
                        isTreeBase(world, logPos) && 
                        !intelligence.isBlacklisted(logPos)) {
                        candidateTrees.add(logPos);
                    }
                }
            }
        }
        
        // If forced exploration mode and we have candidates, prefer distant ones
        if (intelligence.forcedExplorationMode && !candidateTrees.isEmpty()) {
            // Sort by distance (furthest first in forced exploration)
            candidateTrees.sort((a, b) -> {
                double distA = playerPos.getSquaredDistance(a);
                double distB = playerPos.getSquaredDistance(b);
                return Double.compare(distB, distA); // Reverse order for furthest first
            });
            
            // Take the furthest trees that are still reasonable
            for (BlockPos candidate : candidateTrees) {
                double distance = playerPos.getSquaredDistance(candidate);
                if (distance <= searchRadius * searchRadius) { // Within expanded radius
                    double score = intelligence.calculateTreeScore(world, playerPos, candidate);
                    // In forced exploration, give bonus to distant trees
                    score += Math.sqrt(distance) * 5; // Distance bonus
                    if (score > bestScore) {
                        bestScore = score;
                        bestTree = candidate;
                    }
                }
            }
        } else {
            // Normal mode - find best tree by score
            for (BlockPos candidate : candidateTrees) {
                double score = intelligence.calculateTreeScore(world, playerPos, candidate);
                if (score > bestScore) {
                    bestScore = score;
                    bestTree = candidate;
                }
            }
        }
        
        if (bestTree != null) {
            state.currentTask = AIState.Task.MOVING_TO_TREE;
            state.targetTree = bestTree;
            state.ticksSinceLastAction = 0;
            double distance = player.getPos().distanceTo(Vec3d.ofCenter(bestTree));
            
            // Reset forced exploration after finding a target
            if (intelligence.forcedExplorationMode) {
                intelligence.forcedExplorationMode = false;
                ChipperChopperMod.LOGGER.info("Agent.Lumber: Forced exploration successful, found distant tree");
            }
            
            ChipperChopperMod.LOGGER.info("Found tree at: " + bestTree + " (distance: " + String.format("%.2f", distance) + ")");
            intelligence.recordAttempt(bestTree, "New tree target");
            return true;
        }
        
        // No trees found - if not in forced exploration, try it
        if (!intelligence.forcedExplorationMode && intelligence.getFailureCount() > 3) {
            intelligence.forcedExplorationMode = true;
            ChipperChopperMod.LOGGER.info("Agent.Lumber: No nearby trees found, enabling forced exploration mode");
            return findNearestTreeIntelligent(player, state, intelligence); // Recursive call with forced exploration
        }
        
        return false;
    }
    
    private static boolean findNearestTree(ServerPlayerEntity player, AIState state, int radius) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -3; y <= 6; y++) {
                    BlockPos logPos = playerPos.add(x, y, z);
                    
                    if (isLogBlock(world.getBlockState(logPos)) && isTreeBase(world, logPos)) {
                        state.currentTask = AIState.Task.MOVING_TO_TREE;
                        state.targetTree = logPos;
                        state.ticksSinceLastAction = 0;
                        double distance = player.getPos().distanceTo(Vec3d.ofCenter(logPos));
                        ChipperChopperMod.LOGGER.info("Found tree at: " + logPos + " (distance: " + String.format("%.2f", distance) + ")");
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private static boolean findAlternativeApproach(ServerPlayerEntity player, AIState state, AdvancedIntelligence intelligence) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        // Try to find a different log block in the same tree
        BlockPos alternative = findIntelligentTarget(world, playerPos, state.targetTree, state);
        if (alternative != null && !alternative.equals(state.targetTree) && !intelligence.isBlacklisted(alternative)) {
            state.targetTree = alternative;
            state.ticksSinceLastAction = 0;
            startLookingAt(player, state, alternative);
            intelligence.recordProgress("Alternative approach found");
            ChipperChopperMod.LOGGER.info("Agent.Lumber: Found alternative approach: " + alternative);
            return true;
        }
        
        return false;
    }
    
    // Advanced Intelligence System
    private static class AdvancedIntelligence {
        public Set<BlockPos> blacklistedTargets = new HashSet<>();
        public Map<BlockPos, String> blacklistReasons = new HashMap<>();
        public Map<BlockPos, Long> targetFirstAttempted = new HashMap<>();
        public Map<BlockPos, Integer> targetAttemptCounts = new HashMap<>();
        public Map<BlockPos, Long> lastRepositioned = new HashMap<>();
        public Map<BlockPos, Long> alternativesTried = new HashMap<>();
        public Set<BlockPos> problematicBlocks = new HashSet<>();
        public List<BlockPos> positionHistory = new ArrayList<>();
        public int failureCount = 0;
        public int successCount = 0;
        private long lastPositionUpdate = 0;
        
        // NEW: Advanced pattern detection and loop prevention
        public boolean forcedExplorationMode = false;
        public Map<BlockPos, List<Long>> leafClearingAttempts = new HashMap<>(); // Track leaf clearing attempts per tree
        public Map<BlockPos, List<String>> intelligentUpgradeHistory = new HashMap<>(); // Track upgrade patterns
        public Map<BlockPos, Integer> leafClearingFailures = new HashMap<>(); // Count failures per tree
        public Set<BlockPos> loopingTargets = new HashSet<>(); // Targets that are causing loops
        
        public void updatePositionHistory(BlockPos pos) {
            long now = System.currentTimeMillis();
            if (now - lastPositionUpdate > 1000) { // Update every second
                positionHistory.add(pos);
                if (positionHistory.size() > 10) {
                    positionHistory.remove(0); // Keep last 10 positions
                }
                lastPositionUpdate = now;
            }
        }
        
        // NEW: Detect if we're in a leaf clearing loop
        public boolean isInLeafClearingLoop(BlockPos treePos) {
            List<Long> attempts = leafClearingAttempts.get(treePos);
            if (attempts == null || attempts.size() < PATTERN_DETECTION_THRESHOLD) {
                return false;
            }
            
            long now = System.currentTimeMillis();
            // If we've attempted leaf clearing multiple times in the last 30 seconds, it's a loop
            long recentAttempts = attempts.stream()
                .mapToLong(time -> now - time < 30000 ? 1 : 0)
                .sum();
            
            return recentAttempts >= PATTERN_DETECTION_THRESHOLD;
        }
        
        // NEW: Detect if we're in an intelligent upgrade loop
        public boolean isInIntelligentUpgradeLoop(BlockPos treePos) {
            List<String> history = intelligentUpgradeHistory.get(treePos);
            if (history == null || history.size() < PATTERN_DETECTION_THRESHOLD) {
                return false;
            }
            
            // Check for recent repeated upgrades
            int recentUpgrades = 0;
            long now = System.currentTimeMillis();
            for (int i = history.size() - 1; i >= 0 && recentUpgrades < PATTERN_DETECTION_THRESHOLD; i--) {
                String entry = history.get(i);
                // Entry format: "timestamp:target"
                String[] parts = entry.split(":");
                if (parts.length >= 1) {
                    long timestamp = Long.parseLong(parts[0]);
                    if (now - timestamp < 30000) { // Within last 30 seconds
                        recentUpgrades++;
                    } else {
                        break; // Older entries
                    }
                }
            }
            
            return recentUpgrades >= PATTERN_DETECTION_THRESHOLD;
        }
        
        // NEW: Record leaf clearing attempt
        public void recordLeafClearingAttempt(BlockPos treePos, BlockPos leafPos) {
            long now = System.currentTimeMillis();
            leafClearingAttempts.computeIfAbsent(treePos, k -> new ArrayList<>()).add(now);
            
            // Clean old entries (older than 60 seconds)
            List<Long> attempts = leafClearingAttempts.get(treePos);
            attempts.removeIf(time -> now - time > 60000);
        }
        
        // NEW: Record intelligent upgrade for pattern detection
        public void recordIntelligentUpgrade(BlockPos fromTree, BlockPos toTree) {
            long now = System.currentTimeMillis();
            String entry = now + ":" + toTree.toString();
            intelligentUpgradeHistory.computeIfAbsent(fromTree, k -> new ArrayList<>()).add(entry);
            
            // Clean old entries
            List<String> history = intelligentUpgradeHistory.get(fromTree);
            history.removeIf(e -> {
                String[] parts = e.split(":");
                if (parts.length >= 1) {
                    long timestamp = Long.parseLong(parts[0]);
                    return now - timestamp > 60000; // Remove entries older than 60 seconds
                }
                return true;
            });
        }
        
        // NEW: Record leaf clearing failure
        public void recordLeafClearingFailure(BlockPos treePos) {
            leafClearingFailures.put(treePos, leafClearingFailures.getOrDefault(treePos, 0) + 1);
        }
        
        // NEW: Check if tree has recent leaf clearing failures
        public boolean hasRecentLeafClearingFailures(BlockPos treePos) {
            return leafClearingFailures.getOrDefault(treePos, 0) >= 2;
        }
        
        // NEW: Get failure count for forced exploration
        public int getFailureCount() {
            return failureCount;
        }
        
        public boolean isCompletelyStuck() {
            if (positionHistory.size() < 5) return false;
            
            // Check if we've been in the same area for too long
            BlockPos recent = positionHistory.get(positionHistory.size() - 1);
            int sameAreaCount = 0;
            for (int i = positionHistory.size() - 5; i < positionHistory.size(); i++) {
                if (positionHistory.get(i).isWithinDistance(recent, 2.0)) {
                    sameAreaCount++;
                }
            }
            
            return sameAreaCount >= 4; // 4 out of 5 recent positions in same area
        }
        
        public boolean hasMovementStagnated() {
            if (positionHistory.size() < 3) return false;
            
            BlockPos current = positionHistory.get(positionHistory.size() - 1);
            BlockPos twoSecondsAgo = positionHistory.get(Math.max(0, positionHistory.size() - 3));
            
            return current.isWithinDistance(twoSecondsAgo, 1.0);
        }
        
        public void blacklistTarget(BlockPos target, String reason) {
            blacklistedTargets.add(target);
            blacklistReasons.put(target, reason);
            ChipperChopperMod.LOGGER.info("Agent.Lumber: Blacklisted " + target + " (" + reason + ")");
        }
        
        public boolean isBlacklisted(BlockPos target) {
            return blacklistedTargets.contains(target);
        }
        
        public void markProblematic(BlockPos target) {
            problematicBlocks.add(target);
        }
        
        public boolean isProblematic(BlockPos target) {
            return problematicBlocks.contains(target);
        }
        
        public void recordAttempt(BlockPos target, String reason) {
            targetFirstAttempted.putIfAbsent(target, System.currentTimeMillis());
            targetAttemptCounts.put(target, targetAttemptCounts.getOrDefault(target, 0) + 1);
        }
        
        public void recordSuccess(String action) {
            successCount++;
        }
        
        public void recordProgress(String action) {
            // Progress resets failure count for current operation
        }
        
        public void recordFailure(BlockPos target, String reason) {
            failureCount++;
            if (target != null) {
                int attempts = targetAttemptCounts.getOrDefault(target, 0);
                if (attempts > 3) { // Too many attempts
                    blacklistTarget(target, reason + " (too many attempts)");
                }
            }
        }
        
        public boolean shouldAbandonTarget(BlockPos target) {
            if (isBlacklisted(target)) return true;
            
            Long firstAttempt = targetFirstAttempted.get(target);
            if (firstAttempt != null && System.currentTimeMillis() - firstAttempt > 30000) { // 30 seconds
                return true;
            }
            
            Integer attempts = targetAttemptCounts.get(target);
            return attempts != null && attempts > 5;
        }
        
        public void markRepositioned(BlockPos target) {
            lastRepositioned.put(target, System.currentTimeMillis());
        }
        
        public boolean hasRecentlyRepositioned(BlockPos target) {
            Long lastTime = lastRepositioned.get(target);
            return lastTime != null && System.currentTimeMillis() - lastTime < 10000; // 10 seconds
        }
        
        public void markAlternativesTried(BlockPos target) {
            alternativesTried.put(target, System.currentTimeMillis());
        }
        
        public boolean hasRecentlyTriedAlternatives(BlockPos target) {
            Long lastTime = alternativesTried.get(target);
            return lastTime != null && System.currentTimeMillis() - lastTime < 5000; // 5 seconds
        }
        
        public double calculateTreeScore(World world, BlockPos playerPos, BlockPos treePos) {
            double distance = playerPos.getSquaredDistance(treePos);
            double score = 100.0 - distance; // Closer is better
            
            // Bonus for trees we haven't tried recently
            if (!targetFirstAttempted.containsKey(treePos)) {
                score += 50;
            }
            
            // Penalty for previously problematic trees
            Integer attempts = targetAttemptCounts.get(treePos);
            if (attempts != null) {
                score -= attempts * 10;
            }
            
            // Bonus for trees with more logs
            int logCount = countNearbyLogs(world, treePos, 3);
            score += logCount * 5;
            
            // Penalty for trees with many obstructing leaves
            int leafObstacles = countObstructingLeaves(world, playerPos, treePos);
            score -= leafObstacles * 2;
            
            return score;
        }
        
        private int countNearbyLogs(World world, BlockPos center, int radius) {
            int count = 0;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos pos = center.add(x, y, z);
                        if (isLogBlock(world.getBlockState(pos))) {
                            count++;
                        }
                    }
                }
            }
            return count;
        }
        
        public void clearBlacklist() {
            blacklistedTargets.clear();
            blacklistReasons.clear();
            problematicBlocks.clear();
            ChipperChopperMod.LOGGER.info("Agent.Lumber: Intelligence blacklist cleared");
        }
    }
} 