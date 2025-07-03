package com.example.chipper_chopper;

import com.google.common.collect.Lists;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChipperChopperClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("chipper_chopper_client");

    // --- Core AI Systems ---
    private static KeyBinding toggleAIKeyBinding;
    private static final AIContext aiContext = new AIContext();
    private static BehaviorTree behaviorTree;

    // --- HUD System ---
    private static boolean hudEnabled = false;
    private static final List<String> hudMessages = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing ADVANCED Chipper Chopper Client");

        // --- Keybinding Registration ---
        toggleAIKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chipper_chopper.toggle_ai",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.chipper_chopper.general"
        ));

        // --- Build the Behavior Tree ---
        // This tree defines the AI's entire decision-making process in a hierarchical, prioritized way.
        // It reads from top to bottom, left to right.
        behaviorTree = new BehaviorTree(
            new Selector( // A Selector runs its children until one SUCCEEDS. It's for making choices.
                // --- Emergency Handling (Highest Priority) ---
                new Sequence( // A Sequence runs its children until one FAILS. It's for multi-step tasks.
                    new ConditionIsStuck(),
                    new ActionResolveStuck()
                ),
                // --- Server Intelligence Override (High Priority) ---
                new Sequence(
                    new ConditionServerEmergency(),
                    new ActionFollowServerIntelligence()
                ),
                // --- Tree Chopping Logic ---
                new Sequence(
                    new ConditionHasTarget(),
                    new Selector( // Choose what to do with the target
                        // 1. If in range and can see the block, mine it.
                        new Sequence(
                            new ConditionIsInMiningRange(),
                            new ConditionHasLineOfSight(),
                            new ActionMineBlock()
                        ),
                        // 2. If not in mining range, try to move towards it.
                        new Selector( // Try different movement approaches
                            new Sequence(
                                new ActionCalculatePathToTarget(),
                                new ActionFollowPath()
                            ),
                            // If pathfinding fails, invalidate target and try again
                            new ActionInvalidateTarget()
                        )
                    )
                ),
                // --- Find a new tree if we have no target ---
                new ActionFindNewTree(), // Removed path calculation from here
                // --- If all else fails, do nothing ---
                new ActionIdle()
            )
        );

        // --- Client Tick Event with Performance Optimization ---
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Handle AI toggle key press
            while (toggleAIKeyBinding.wasPressed()) {
                aiContext.toggleAI(client);
                hudEnabled = aiContext.isActive();
                if (!hudEnabled) {
                    releaseAllKeys(client.options);
                }
            }

            // PERFORMANCE OPTIMIZATION: Reduce AI processing frequency
            if (aiContext.isActive() && aiContext.shouldProcessAI()) {
                // Process AI only every few ticks instead of every tick
                behaviorTree.tick(client, aiContext);
                aiContext.markProcessed();
            }

            // PERFORMANCE OPTIMIZATION: Throttle HUD updates to 10 FPS
            if (hudEnabled && aiContext.shouldUpdateHUD()) {
                updateHUD(client);
                aiContext.markHUDUpdated();
            }
        });

        // --- HUD Rendering ---
        HudRenderCallback.EVENT.register(this::renderHUD);

        LOGGER.info("Chipper Chopper Client initialized with Behavior Tree and A* Pathfinding.");
    }

    /**
     * Resets all movement and action keys to false.
     * @param opts The game options containing key bindings.
     */
    public static void releaseAllKeys(GameOptions opts) {
        opts.forwardKey.setPressed(false);
        opts.backKey.setPressed(false);
        opts.leftKey.setPressed(false);
        opts.rightKey.setPressed(false);
        opts.jumpKey.setPressed(false);
        opts.sprintKey.setPressed(false);
        if (MinecraftClient.getInstance().interactionManager != null) {
            MinecraftClient.getInstance().interactionManager.cancelBlockBreaking();
        }
    }

    // =================================================================================
    //  HUD SYSTEM
    //  Renders the AI status display on the screen.
    // =================================================================================

    private void updateHUD(MinecraftClient client) {
        hudMessages.clear();
        hudMessages.add("§b§lAgent.Lumber v2.6.1 (Advanced + Intelligence)§r");
        hudMessages.add("§7═════════════════════════════");
        hudMessages.add(aiContext.isActive() ? "§a● §lSTATUS: ACTIVE" : "§c● §lSTATUS: INACTIVE");
        hudMessages.add(String.format("§e§lSTATE: §r§f%s", aiContext.getCurrentState()));

        if (aiContext.isActive()) {
            hudMessages.add("");
            if (aiContext.getTargetTreePos() != null) {
                hudMessages.add(String.format("§6§lTREE TARGET: §r(§f%d, %d, %d§r)", aiContext.getTargetTreePos().getX(), aiContext.getTargetTreePos().getY(), aiContext.getTargetTreePos().getZ()));
            } else {
                hudMessages.add("§6§lTREE TARGET: §r§cNone");
            }

            if (aiContext.getCurrentMiningTarget() != null) {
                hudMessages.add(String.format("§2§lMINING BLOCK: §r(§f%d, %d, %d§r)", aiContext.getCurrentMiningTarget().getX(), aiContext.getCurrentMiningTarget().getY(), aiContext.getCurrentMiningTarget().getZ()));
            }

            if (aiContext.getAStarPath() != null && !aiContext.getAStarPath().isEmpty()) {
                hudMessages.add(String.format("§3§lPATH: §r§aFollowing §7(§f%d waypoints§7)§r", aiContext.getAStarPath().size()));
            } else {
                 hudMessages.add("§3§lPATH: §r§eIdle or Calculating");
            }
            
            // NEW: Display behavior tree debugging info
            hudMessages.add("");
            hudMessages.add("§d§lBEHAVIOR TREE DEBUG:§r");
            
            // Show current behavior tree decision
            String btDecision = "Unknown";
            if (aiContext.getTargetTreePos() != null) {
                if (aiContext.getAStarPath() != null && !aiContext.getAStarPath().isEmpty()) {
                    btDecision = "Following Path";
                } else if (aiContext.getCurrentMiningTarget() != null) {
                    btDecision = "Mining Target";
                } else {
                    btDecision = "Calculating Path";
                }
            } else {
                btDecision = "Finding Tree";
            }
            hudMessages.add(String.format("§7BT Decision: §f%s", btDecision));
            
            // NEW: Display server intelligence information
            hudMessages.add("");
            hudMessages.add("§d§lSERVER INTELLIGENCE:§r");
            
            // Get thinking state from server
            try {
                String thinkingState = TreeChopperAI.getCurrentThinkingState(client.player).getDescription();
                hudMessages.add(String.format("§7AI Thinking: §f%s", thinkingState));
            } catch (Exception e) {
                hudMessages.add("§7AI Thinking: §cUnavailable");
            }
            
            // Check if server is doing special actions
            if (TreeChopperAI.isRepositioning(client.player)) {
                hudMessages.add("§e⚡ Server is repositioning");
            }
            if (TreeChopperAI.isClearingLeaves(client.player)) {
                hudMessages.add("§a🌿 Server is clearing leaves");
            }
            
            // Show server target status
            try {
                Vec3d serverTarget = TreeChopperAI.getActiveTarget(client.player);
                if (serverTarget != null) {
                    BlockPos serverPos = BlockPos.ofFloored(serverTarget);
                    hudMessages.add(String.format("§7Server Target: §f(%d, %d, %d)", serverPos.getX(), serverPos.getY(), serverPos.getZ()));
                } else {
                    hudMessages.add("§7Server Target: §cNone");
                }
            } catch (Exception e) {
                hudMessages.add("§7Server Target: §cError");
            }
        }
        hudMessages.add("§7═════════════════════════════");
        hudMessages.add("§7Press 'G' to toggle AI");
    }

    private void renderHUD(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        if (!hudEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int y = 10;

        for (int i = 0; i < hudMessages.size(); i++) {
            String message = hudMessages.get(i);
            int x = 10;
            int textY = y + (i * 12);
            context.fill(x - 2, textY - 2, x + textRenderer.getWidth(message) + 2, textY + 10, 0x90000000);
            context.drawTextWithShadow(textRenderer, message, x, textY, 0xFFFFFF);
        }
    }
}

// =================================================================================
//  AI CONTEXT
//  A central class to hold all the AI's state and data.
// =================================================================================
class AIContext {
    private boolean isActive = false;
    private String currentState = "Idle";
    private BlockPos targetTreePos = null;
    private BlockPos currentMiningTarget = null;
    private List<BlockPos> aStarPath = null;

    // Stuck detection
    private Vec3d lastPosition = Vec3d.ZERO;
    private int ticksStuck = 0;
    private static final int STUCK_THRESHOLD = 60; // 3 seconds

    // Timers & Cooldowns
    private long lastMineTime = 0;
    private static final long MINE_TIMEOUT = 4000; // 4 seconds
    
    // PERFORMANCE OPTIMIZATION: Tick frequency control
    private int aiTickCounter = 0;
    private int hudTickCounter = 0;
    private static final int AI_PROCESS_INTERVAL = 4; // Process AI every 4 client ticks
    private static final int HUD_UPDATE_INTERVAL = 6; // Update HUD at 10 FPS (60/6)

    public void toggleAI(MinecraftClient client) {
        this.isActive = !this.isActive;
        if (this.isActive) {
            this.currentState = "Initializing...";
            this.lastPosition = client.player.getPos();
            client.player.sendMessage(Text.literal("§aChipper Chopper AI Activated."), true);
            
            // SYNC: Start server-side AI when client AI is activated
            client.player.networkHandler.sendCommand("chipper start");
        } else {
            this.currentState = "Inactive";
            reset();
            client.player.sendMessage(Text.literal("§cChipper Chopper AI Deactivated."), true);
            
            // SYNC: Stop server-side AI when client AI is deactivated
            client.player.networkHandler.sendCommand("chipper stop");
        }
    }

    public void reset() {
        this.targetTreePos = null;
        this.currentMiningTarget = null;
        this.aStarPath = null;
        this.ticksStuck = 0;
    }

    // --- Getters & Setters ---
    public boolean isActive() { return isActive; }
    public String getCurrentState() { return currentState; }
    public void setCurrentState(String state) { this.currentState = state; }
    public BlockPos getTargetTreePos() { return targetTreePos; }
    public void setTargetTreePos(BlockPos pos) { this.targetTreePos = pos; }
    public BlockPos getCurrentMiningTarget() { return currentMiningTarget; }
    public void setCurrentMiningTarget(BlockPos pos) { this.currentMiningTarget = pos; }
    public List<BlockPos> getAStarPath() { return aStarPath; }
    public void setAStarPath(List<BlockPos> path) { this.aStarPath = path; }
    public Vec3d getLastPosition() { return lastPosition; }
    public void setLastPosition(Vec3d pos) { this.lastPosition = pos; }
    public int getTicksStuck() { return ticksStuck; }
    public void incrementTicksStuck() { this.ticksStuck++; }
    public void resetTicksStuck() { this.ticksStuck = 0; }
    public long getLastMineTime() { return lastMineTime; }
    public void setLastMineTime(long time) { this.lastMineTime = time; }

    public static int getStuckThreshold() { return STUCK_THRESHOLD; }
    public static long getMineTimeout() { return MINE_TIMEOUT; }
    
    // PERFORMANCE OPTIMIZATION: Tick frequency control methods
    public boolean shouldProcessAI() {
        aiTickCounter++;
        return aiTickCounter >= AI_PROCESS_INTERVAL;
    }
    
    public void markProcessed() {
        aiTickCounter = 0;
    }
    
    public boolean shouldUpdateHUD() {
        hudTickCounter++;
        return hudTickCounter >= HUD_UPDATE_INTERVAL;
    }
    
    public void markHUDUpdated() {
        hudTickCounter = 0;
    }
}

// =================================================================================
//  BEHAVIOR TREE (BT) FRAMEWORK
// =================================================================================

enum BTStatus { SUCCESS, FAILURE, RUNNING }

/** Base interface for all nodes in the Behavior Tree. */
interface BTNode {
    BTStatus tick(MinecraftClient client, AIContext context);
}

/** A composite node that runs children in order until one succeeds. Returns SUCCESS if a child succeeds, FAILURE if all fail. */
class Selector implements BTNode {
    private final List<BTNode> children;
    public Selector(BTNode... children) { this.children = Lists.newArrayList(children); }

    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        for (BTNode child : children) {
            BTStatus status = child.tick(client, context);
            if (status != BTStatus.FAILURE) {
                return status;
            }
        }
        return BTStatus.FAILURE;
    }
}

/** A composite node that runs children in order until one fails. Returns FAILURE if any child fails, SUCCESS if all succeed. */
class Sequence implements BTNode {
    private final List<BTNode> children;
    public Sequence(BTNode... children) { this.children = Lists.newArrayList(children); }

    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        for (BTNode child : children) {
            BTStatus status = child.tick(client, context);
            if (status != BTStatus.SUCCESS) {
                return status;
            }
        }
        return BTStatus.SUCCESS;
    }
}

/** The main Behavior Tree class that holds the root node. */
class BehaviorTree {
    private final BTNode root;
    public BehaviorTree(BTNode root) { this.root = root; }
    public void tick(MinecraftClient client, AIContext context) {
        root.tick(client, context);
    }
}


// =================================================================================
//  BEHAVIOR TREE NODES - CONDITIONS (The "IF" statements of the AI)
// =================================================================================

class ConditionHasTarget implements BTNode {
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        return context.getTargetTreePos() != null ? BTStatus.SUCCESS : BTStatus.FAILURE;
    }
}

class ConditionIsInMiningRange implements BTNode {
    private static final double MINING_RANGE_SQUARED = 6.0 * 6.0; // Use squared distance for efficiency

    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        BlockPos target = context.getCurrentMiningTarget();
        if (target == null) {
            target = context.getTargetTreePos();
        }
        if (target != null && client.player.getPos().squaredDistanceTo(Vec3d.ofCenter(target)) <= MINING_RANGE_SQUARED) {
            return BTStatus.SUCCESS;
        }
        return BTStatus.FAILURE;
    }
}

class ConditionHasLineOfSight implements BTNode {
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        // NEW: Get mining target from server-side intelligence
        BlockPos serverMiningTarget = TreeChopperAI.getCurrentMiningTarget(client.player);
        if (serverMiningTarget != null) {
            context.setCurrentMiningTarget(serverMiningTarget);
            
            Vec3d eyePos = client.player.getEyePos();
            Vec3d blockCenter = Vec3d.ofCenter(serverMiningTarget);

            BlockHitResult result = client.world.raycast(new RaycastContext(
                    eyePos, blockCenter, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));

            if (result != null && result.getBlockPos().equals(serverMiningTarget)) {
                return BTStatus.SUCCESS;
            } else {
                // Notify server of line of sight failure for intelligence learning
                TreeChopperAI.notifyLineOfSightFailure(client.player);
                return BTStatus.FAILURE;
            }
        }
        
        // Fallback: Find the next log to mine locally
        BlockPos logToMine = findNextLog(client, context.getTargetTreePos());
        if (logToMine == null) {
            // No more logs, maybe the tree is done
            context.setTargetTreePos(null);
            return BTStatus.FAILURE;
        }
        context.setCurrentMiningTarget(logToMine);

        Vec3d eyePos = client.player.getEyePos();
        Vec3d blockCenter = Vec3d.ofCenter(logToMine);

        BlockHitResult result = client.world.raycast(new RaycastContext(
                eyePos, blockCenter, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player));

        if (result != null && result.getBlockPos().equals(logToMine)) {
            return BTStatus.SUCCESS;
        }
        return BTStatus.FAILURE;
    }

    private BlockPos findNextLog(MinecraftClient client, BlockPos base) {
        if (base == null) return null;
        
        // Simplistic scan: find lowest log block starting from the base
        for (int i = 0; i < 10; i++) {
            BlockPos current = base.up(i);
            BlockState state = client.world.getBlockState(current);
            // This check should be more robust (e.g., using block tags #LOGS)
            if (!state.isAir() && state.getBlock().getName().getString().toLowerCase().contains("log")) {
                return current;
            }
        }
        return null;
    }
}

class ConditionIsStuck implements BTNode {
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        // Only check for stuck if we have a path we are trying to follow
        if (context.getAStarPath() != null && !context.getAStarPath().isEmpty()) {
            if (client.player.getPos().distanceTo(context.getLastPosition()) < 0.05) {
                context.incrementTicksStuck();
            } else {
                context.resetTicksStuck();
                context.setLastPosition(client.player.getPos());
            }

            if (context.getTicksStuck() > AIContext.getStuckThreshold()) {
                return BTStatus.SUCCESS;
            }
        }
        return BTStatus.FAILURE;
    }
}

// NEW: Condition to check if server intelligence recommends emergency action
class ConditionServerEmergency implements BTNode {
    private static long lastEmergencyTime = 0;
    private static final long EMERGENCY_COOLDOWN = 2000; // 2 second cooldown
    
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        // Don't trigger emergency too frequently
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastEmergencyTime < EMERGENCY_COOLDOWN) {
            return BTStatus.FAILURE;
        }
        
        // Only check for server emergency if we're actually trying to do something
        if (context.getTargetTreePos() == null) {
            return BTStatus.FAILURE; // No point in emergency mode if we have no target anyway
        }
        
        // Check if server is in emergency mode or has recommended abandoning current target
        Vec3d serverTarget = TreeChopperAI.getActiveTarget(client.player);
        BlockPos currentTarget = context.getTargetTreePos();
        
        // If we have a target but server doesn't, server may have abandoned it
        // BUT only trigger if we've been trying for a while
        if (currentTarget != null && serverTarget == null) {
            // Check if server is actually inactive or just temporarily without target
            try {
                if (!TreeChopperAI.isActive(client.player)) {
                    // Server AI is actually off - this is expected, not an emergency
                    return BTStatus.FAILURE;
                }
            } catch (Exception e) {
                // If we can't check server state, be conservative
                return BTStatus.FAILURE;
            }
            
            context.setCurrentState("Server intelligence recommends target abandonment");
            lastEmergencyTime = currentTime;
            return BTStatus.SUCCESS;
        }
        
        // If server target differs significantly from our target, we should follow server
        if (currentTarget != null && serverTarget != null) {
            BlockPos serverTreePos = BlockPos.ofFloored(serverTarget);
            if (!serverTreePos.equals(currentTarget) && 
                currentTarget.getSquaredDistance(serverTreePos) > 100) { // More than 10 blocks difference
                context.setCurrentState("Server intelligence found better target");
                lastEmergencyTime = currentTime;
                return BTStatus.SUCCESS;
            }
        }
        
        return BTStatus.FAILURE;
    }
}


// =================================================================================
//  BEHAVIOR TREE NODES - ACTIONS (The "DO" statements of the AI)
// =================================================================================

class ActionIdle implements BTNode {
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        context.setCurrentState("Idle");
        ChipperChopperClient.releaseAllKeys(client.options);
        return BTStatus.RUNNING; // Idle is a continuous state
    }
}

class ActionFindNewTree implements BTNode {
    private static long lastSearchTime = 0;
    private static final long SEARCH_COOLDOWN = 1000; // 1 second cooldown between searches
    
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        // Don't search too frequently
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSearchTime < SEARCH_COOLDOWN) {
            context.setCurrentState("Waiting before next tree search...");
            return BTStatus.FAILURE; // Wait before searching again
        }
        
        context.setCurrentState("Scanning for new tree...");
        lastSearchTime = currentTime;
        
        // NEW: Get target from server-side intelligence system
        Vec3d serverTarget = TreeChopperAI.getActiveTarget(client.player);
        if (serverTarget != null) {
            BlockPos serverTreePos = BlockPos.ofFloored(serverTarget);
            // Validate it's actually a tree
            BlockState state = client.world.getBlockState(serverTreePos);
            if (state.getBlock().getName().getString().toLowerCase().contains("log")) {
                context.setTargetTreePos(serverTreePos);
                context.setCurrentState("Using server intelligence target");
                return BTStatus.SUCCESS;
            }
        }
        
        // Fallback: Local tree search if server doesn't provide target
        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                for (int y = -5; y <= 5; y++) {
                    BlockPos potentialTreePos = client.player.getBlockPos().add(x, y, z);
                    BlockState state = client.world.getBlockState(potentialTreePos);
                    if (state.getBlock().getName().getString().toLowerCase().contains("log")) {
                        context.setTargetTreePos(potentialTreePos);
                        context.setCurrentState("Found local tree target");
                        return BTStatus.SUCCESS;
                    }
                }
            }
        }
        context.setCurrentState("No trees found in range.");
        return BTStatus.FAILURE;
    }
}

class ActionInvalidateTarget implements BTNode {
    private static long lastInvalidationTime = 0;
    private static final long INVALIDATION_COOLDOWN = 500; // 0.5 second cooldown
    
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        // Don't invalidate too frequently
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInvalidationTime < INVALIDATION_COOLDOWN) {
            context.setCurrentState("Delaying target invalidation...");
            return BTStatus.RUNNING; // Keep trying for a bit
        }
        
        lastInvalidationTime = currentTime;
        context.setCurrentState("Target is unreachable or invalid. Finding new target.");
        context.reset(); // Clear target, path, etc.
        return BTStatus.SUCCESS;
    }
}

class ActionCalculatePathToTarget implements BTNode {
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        if (context.getTargetTreePos() == null) return BTStatus.FAILURE;

        context.setCurrentState("Calculating path to target...");
        BlockPos start = client.player.getBlockPos();
        BlockPos end = context.getTargetTreePos();

        // Try to find a safe spot near the tree to stand on
        BlockPos destination = findSafeStandableSpot(client, end);
        if (destination == null) {
            // Fallback: Try to path directly to the tree base
            destination = end;
            context.setCurrentState("No safe spot found, trying direct approach...");
        }

        AStarPathfinder pathfinder = new AStarPathfinder(1000); // Limit search to 1000 nodes
        List<BlockPos> path = pathfinder.findPath(client, start, destination);

        if (path != null && !path.isEmpty()) {
            context.setAStarPath(path);
            context.setCurrentState("Path calculated. Moving...");
            return BTStatus.SUCCESS;
        } else {
            // Fallback: If pathfinding fails, just try to move directly
            List<BlockPos> directPath = createDirectPath(start, destination);
            if (directPath != null && !directPath.isEmpty()) {
                context.setAStarPath(directPath);
                context.setCurrentState("Using direct path approach...");
                return BTStatus.SUCCESS;
            } else {
                context.setCurrentState("All pathfinding attempts failed.");
                context.setAStarPath(null);
                return BTStatus.FAILURE;
            }
        }
    }

    private BlockPos findSafeStandableSpot(MinecraftClient client, BlockPos treeBase) {
        // First try original method
        for (int r = 1; r < 8; r++) { // Increased search radius
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue; // Only check the perimeter of the radius
                    
                    BlockPos candidate = treeBase.add(dx, 0, dz);
                    if (isSafePosition(client, candidate)) {
                        return candidate;
                    }
                    
                    // Also try one block up and down
                    if (isSafePosition(client, candidate.up())) {
                        return candidate.up();
                    }
                    if (isSafePosition(client, candidate.down())) {
                        return candidate.down();
                    }
                }
            }
        }
        
        // Fallback: Just return the tree base if nothing else works
        return treeBase;
    }
    
    private boolean isSafePosition(MinecraftClient client, BlockPos pos) {
        // More lenient safety check
        try {
            BlockState ground = client.world.getBlockState(pos.down());
            BlockState feet = client.world.getBlockState(pos);
            BlockState head = client.world.getBlockState(pos.up());
            
            // Must have solid ground and space for player
            return ground.isSolid() && !feet.isSolid() && !head.isSolid();
        } catch (Exception e) {
            return false;
        }
    }
    
    private List<BlockPos> createDirectPath(BlockPos start, BlockPos end) {
        List<BlockPos> path = new ArrayList<>();
        
        // Simple direct line path as last resort
        int dx = end.getX() - start.getX();
        int dz = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        
        if (steps == 0) {
            path.add(end);
            return path;
        }
        
        for (int i = 1; i <= steps; i++) {
            int x = start.getX() + (dx * i / steps);
            int z = start.getZ() + (dz * i / steps);
            int y = start.getY(); // Keep same Y level initially
            path.add(new BlockPos(x, y, z));
        }
        
        path.add(end); // Ensure we end at the target
        return path;
    }
}

class ActionFollowPath implements BTNode {
    private static final float TURN_SPEED = 8.0f;
    private static final double WAYPOINT_REACHED_DISTANCE = 1.2;

    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        List<BlockPos> path = context.getAStarPath();
        if (path == null || path.isEmpty()) {
            context.setCurrentState("Path completed or invalid.");
            ChipperChopperClient.releaseAllKeys(client.options);
            return BTStatus.SUCCESS; // We reached the end of the path
        }
        
        context.setCurrentState("Following path...");
        BlockPos nextWaypoint = path.get(0);
        Vec3d waypointCenter = Vec3d.ofCenter(nextWaypoint);
        Vec3d playerPos = client.player.getPos();
        
        // Check if we've reached the current waypoint
        if (playerPos.distanceTo(waypointCenter) < WAYPOINT_REACHED_DISTANCE) {
            path.remove(0); // Move to the next waypoint
            if (path.isEmpty()) {
                // Path complete!
                ChipperChopperClient.releaseAllKeys(client.options);
                return BTStatus.SUCCESS;
            }
            nextWaypoint = path.get(0);
            waypointCenter = Vec3d.ofCenter(nextWaypoint);
        }

        // --- Movement Logic ---
        GameOptions opts = client.options;
        Vec3d directionToWaypoint = waypointCenter.subtract(playerPos);

        // Smooth Rotation
        float targetYaw = (float) (Math.toDegrees(Math.atan2(directionToWaypoint.z, directionToWaypoint.x)) - 90f);
        client.player.setYaw(lerpAngle(client.player.getYaw(), targetYaw, TURN_SPEED));

        // Simple Movement Keys
        opts.forwardKey.setPressed(true);
        opts.sprintKey.setPressed(playerPos.distanceTo(Vec3d.ofCenter(context.getTargetTreePos())) > 10); // Sprint if far away

        // Simple Jump Logic: Jump if the next waypoint is above us.
        opts.jumpKey.setPressed(nextWaypoint.getY() > client.player.getBlockPos().getY() && client.player.isOnGround());

        return BTStatus.RUNNING; // We are still following the path
    }

    private float lerpAngle(float startAngle, float endAngle, float speed) {
        float diff = endAngle - startAngle;
        while (diff < -180.0f) diff += 360.0f;
        while (diff >= 180.0f) diff -= 360.0f;
        return startAngle + diff / speed;
    }
}

class ActionMineBlock implements BTNode {
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        BlockPos target = context.getCurrentMiningTarget();
        if (target == null) return BTStatus.FAILURE;

        context.setCurrentState("Mining block...");
        ChipperChopperClient.releaseAllKeys(client.options); // Stop moving

        // Face the block
        faceBlock(client, target);
        
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        // Check if we just started mining this block
        if (context.getLastMineTime() == 0) {
             context.setLastMineTime(System.currentTimeMillis());
             interactionManager.attackBlock(target, Direction.UP); // Start breaking
        }
        
        // Update breaking progress
        interactionManager.updateBlockBreakingProgress(target, Direction.UP);
        
        // NEW: Notify server of mining progress for intelligence learning
        if (System.currentTimeMillis() - context.getLastMineTime() > 500) { // Every 0.5 seconds
            TreeChopperAI.notifyMiningProgress(client.player);
        }

        // Check for timeout
        if (System.currentTimeMillis() - context.getLastMineTime() > AIContext.getMineTimeout()) {
            context.setCurrentState("Mining timed out, requesting new target from server.");
            // NEW: Ask server intelligence to find alternative
            TreeChopperAI.forceNewTarget(client.player);
            context.setCurrentMiningTarget(null);
            context.setLastMineTime(0);
            interactionManager.cancelBlockBreaking();
            return BTStatus.FAILURE;
        }

        // Check if block is gone
        if (client.world.getBlockState(target).isAir()) {
            context.setCurrentState("Block broken.");
            context.setCurrentMiningTarget(null);
            context.setLastMineTime(0);
            return BTStatus.SUCCESS;
        }

        return BTStatus.RUNNING; // Still mining
    }

    private void faceBlock(MinecraftClient mc, BlockPos blockPos) {
        Vec3d playerEyePos = mc.player.getEyePos();
        Vec3d blockCenter = Vec3d.ofCenter(blockPos);
        Vec3d direction = blockCenter.subtract(playerEyePos).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }
}

class ActionResolveStuck implements BTNode {
    private int resolveTicks = 0;
    
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        context.setCurrentState("Attempting to un-stuck...");
        
        if (resolveTicks == 0) {
            // On first tick, invalidate the path and start moving backward
            context.setAStarPath(null);
            client.options.backKey.setPressed(true);
            client.options.jumpKey.setPressed(true);
        }

        resolveTicks++;

        if (resolveTicks > 20) { // After 1 second of trying
            ChipperChopperClient.releaseAllKeys(client.options);
            context.resetTicksStuck();
            resolveTicks = 0;
            // Now the BT will try to find a new path or a new tree.
            return BTStatus.SUCCESS; 
        }

        return BTStatus.RUNNING;
    }
}

// NEW: Action to follow server intelligence recommendations
class ActionFollowServerIntelligence implements BTNode {
    @Override
    public BTStatus tick(MinecraftClient client, AIContext context) {
        // First check if server AI is actually active
        try {
            if (!TreeChopperAI.isActive(client.player)) {
                // Server AI is off, so ignore any stale server recommendations
                context.setCurrentState("Server AI is inactive, proceeding with client logic");
                return BTStatus.FAILURE; // Let other behavior tree nodes handle this
            }
        } catch (Exception e) {
            // If we can't check server state, be conservative and proceed with client logic
            return BTStatus.FAILURE;
        }
        
        Vec3d serverTarget = TreeChopperAI.getActiveTarget(client.player);
        
        if (serverTarget != null) {
            BlockPos serverTreePos = BlockPos.ofFloored(serverTarget);
            context.setTargetTreePos(serverTreePos);
            context.setCurrentMiningTarget(null); // Reset mining target
            context.setAStarPath(null); // Force new path calculation
            context.setCurrentState("Following server intelligence recommendation");
            return BTStatus.SUCCESS;
        } else {
            // Server recommends no target - reset everything
            context.reset();
            context.setCurrentState("Server recommends idle state");
            return BTStatus.SUCCESS;
        }
    }
}

// =================================================================================
//  A* PATHFINDING IMPLEMENTATION
// =================================================================================

class AStarPathfinder {
    
    private final int maxNodesToVisit;
    
    // PERFORMANCE OPTIMIZATION: Path caching
    private static final Map<String, CachedPath> pathCache = new HashMap<>();
    private static final long CACHE_DURATION_MS = 10000; // 10 seconds cache
    
    public AStarPathfinder(int maxNodesToVisit) {
        this.maxNodesToVisit = maxNodesToVisit;
    }
    
    private static class CachedPath {
        public final List<BlockPos> path;
        public final long timestamp;
        
        CachedPath(List<BlockPos> path) {
            this.path = new ArrayList<>(path);
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION_MS;
        }
    }

    public List<BlockPos> findPath(MinecraftClient client, BlockPos start, BlockPos end) {
        // PERFORMANCE OPTIMIZATION: Check cache first
        String cacheKey = start.toString() + "->" + end.toString();
        CachedPath cached = pathCache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return new ArrayList<>(cached.path);
        }
        
        // Clean up old cache entries periodically
        if (pathCache.size() > 20) {
            pathCache.entrySet().removeIf(entry -> !entry.getValue().isValid());
        }
        
        Node startNode = new Node(start, null, 0, getHeuristic(start, end));
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::getFCost));
        Map<BlockPos, Node> allNodes = new HashMap<>();

        openSet.add(startNode);
        allNodes.put(start, startNode);

        int visitedCount = 0;
        // PERFORMANCE OPTIMIZATION: Reduced max nodes for faster pathfinding
        int maxNodes = Math.min(maxNodesToVisit, 300);

        while (!openSet.isEmpty()) {
            if (visitedCount++ > maxNodes) {
                return null; // Path too long or complex
            }

            Node current = openSet.poll();

            if (current.position.equals(end)) {
                List<BlockPos> path = reconstructPath(current);
                // Cache the successful path
                pathCache.put(cacheKey, new CachedPath(path));
                return path;
            }

            // PERFORMANCE OPTIMIZATION: Only check 4 cardinal directions for speed
            BlockPos[] neighbors = {
                current.position.add(1, 0, 0), current.position.add(-1, 0, 0),
                current.position.add(0, 0, 1), current.position.add(0, 0, -1)
            };

            for (BlockPos neighborPos : neighbors) {
                // Simple gravity handling
                if (isTraversable(client, neighborPos.down())) {
                    neighborPos = neighborPos.down();
                } else if (isTraversable(client, neighborPos) && !isTraversable(client, neighborPos.down())) {
                    // This is a valid step
                } else if (isTraversable(client, neighborPos.up()) && isTraversable(client, neighborPos)) {
                    // This is a jump-up
                    neighborPos = neighborPos.up();
                } else {
                    continue; // Can't move here
                }

                double tentativeGCost = current.gCost + getMovementCost(current.position, neighborPos);
                Node neighborNode = allNodes.get(neighborPos);

                if (neighborNode == null || tentativeGCost < neighborNode.gCost) {
                    if (neighborNode == null) {
                        neighborNode = new Node(neighborPos);
                        allNodes.put(neighborPos, neighborNode);
                    }
                    neighborNode.parent = current;
                    neighborNode.gCost = tentativeGCost;
                    neighborNode.hCost = getHeuristic(neighborPos, end);
                    
                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode);
                    }
                }
            }
        }
        return null; // No path found
    }

    private boolean isTraversable(MinecraftClient client, BlockPos pos) {
        // A block is traversable if it's not solid and the block below it is solid (for standing)
        // This is a simplification; a real one would handle water, slabs, etc.
        return !client.world.getBlockState(pos).isSolid() && 
               !client.world.getBlockState(pos.up()).isSolid(); // Ensure head-room
    }

    private double getMovementCost(BlockPos from, BlockPos to) {
        // Diagonal movement costs more
        double dist = from.getManhattanDistance(to);
        if (dist > 1) return 1.4; // Diagonal
        return 1.0;
    }

    private double getHeuristic(BlockPos pos, BlockPos end) {
        return Math.sqrt(pos.getSquaredDistance(end)); // Euclidean distance
    }



    private List<BlockPos> reconstructPath(Node endNode) {
        List<BlockPos> path = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            path.add(current.position);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static class Node {
        BlockPos position;
        Node parent;
        double gCost; // Cost from start
        double hCost; // Heuristic cost to end

        public Node(BlockPos position, Node parent, double gCost, double hCost) {
            this.position = position;
            this.parent = parent;
            this.gCost = gCost;
            this.hCost = hCost;
        }
        
        public Node(BlockPos position) {
            this(position, null, Double.MAX_VALUE, Double.MAX_VALUE);
        }

        public double getFCost() {
            return gCost + hCost;
        }
    }
}