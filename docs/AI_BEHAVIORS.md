# 🧠 AI Behaviors & Decision Making

## Overview
Chipper Chopper's AI uses a sophisticated state machine with intelligent decision making to create realistic, player-like tree harvesting behavior. This document explains how the AI thinks and acts.

## 🎯 AI States

### 🔍 IDLE
**What it does**: Scans the environment for opportunities
- Searches for trees within 16-block radius
- Looks for dropped items to collect
- Prioritizes tree chopping over item collection
- Resets collection attempts to prevent getting stuck

### 🚶 MOVING_TO_TREE
**What it does**: Navigates toward a selected tree
- Uses physics-based movement with acceleration/deceleration
- Maintains line-of-sight to target while moving
- Switches to CLEARING_LEAVES if leaves block the path
- Gives up if tree is too far away (50+ blocks)

### ✂️ CLEARING_LEAVES (NEW!)
**What it does**: Removes obstructing leaves before chopping
- **Raycast Detection**: Uses line-of-sight checks to find blocking leaves
- **Strategic Targeting**: Focuses on specific leaf blocks obstructing the view
- **Smart Repositioning**: Moves to better positions if current angle is blocked
- **All Leaf Types**: Recognizes all Minecraft leaf variants (Oak, Birch, Spruce, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Pale Oak)

### 🪓 CHOPPING
**What it does**: Systematically harvests tree blocks
- **Bottom-Up Strategy**: Starts from tree base and works upward
- **Continuous Monitoring**: Checks for new leaf obstructions
- **Position Optimization**: Moves underneath trees for better angles
- **2x2 Tree Support**: Handles large trees (Dark Oak, Jungle) completely
- **Timeout Handling**: Finds alternative blocks if stuck (120 ticks = 6 seconds)

### 🎯 REPOSITIONING
**What it does**: Moves to better positions when stuck
- **Under-Tree Positioning**: Finds spots underneath tree canopy
- **Leaf-Clearing Positions**: Moves to angles with fewer obstructing leaves
- **Accessibility Checking**: Validates that new positions are reachable
- **Timeout Protection**: Gives up after 10 seconds to prevent infinite loops

### 📦 COLLECTING
**What it does**: Gathers dropped items from harvested trees
- **Active Search**: Scans 8-block radius for item entities
- **Pathfinding**: Actually moves toward items (not just proximity detection)
- **Priority System**: Goes for closest items first
- **Realistic Collection**: Respects Minecraft's pickup range (2 blocks)

### 🏃 MOVING_TO_ITEM
**What it does**: Pursues specific dropped items
- **Direct Navigation**: Moves straight toward target item
- **Distance Checking**: Gives up if item is too far or despawned
- **Collection Transition**: Switches to collecting when close enough

## 🧠 Decision Making Logic

### Tree Selection
1. **Proximity**: Closer trees get priority
2. **Accessibility**: Checks if tree base is reachable
3. **Type Recognition**: Identifies 1x1 vs 2x2 tree structures
4. **Ground Level**: Prefers trees at or near ground level

### Leaf Clearing Strategy
```
Player Position → Raycast → Tree Target
      ↓
  Find First Leaf Block in Path
      ↓
  Focus Mining on That Leaf
      ↓
  Check if More Leaves Block Path
      ↓
  Repeat Until Clear Line of Sight
```

### Positioning Logic
1. **Line-of-Sight Check**: Can I see the target block?
2. **Leaf Interference**: Are leaves blocking my view?
3. **Angle Optimization**: Is there a better position?
4. **Under-Tree Check**: Can I get underneath for upward mining?

### Problem Solving Hierarchy
1. **Try Current Position**: Attempt mining from current spot
2. **Clear Leaves**: Remove obstructing leaves if present
3. **Reposition Nearby**: Move to better angle (same level)
4. **Go Underneath**: Move below tree for upward mining
5. **Find Alternative Block**: Switch to different accessible log
6. **Give Up Tree**: Move to different tree entirely

## 🎮 Player-Like Behaviors

### Realistic Movement
- **Acceleration/Deceleration**: Smooth speed changes
- **Turn Speed Limiting**: No instant 180° rotations
- **Jump Intelligence**: Only jumps when necessary
- **Momentum Physics**: Feels natural, not robotic

### Mining Authenticity
- **Hold-to-Mine**: Simulates holding down mouse button
- **Progress Tracking**: Respects Minecraft's mining mechanics
- **Tool Requirements**: Works with player's equipped tools
- **Break Validation**: Proper block breaking with particles

### Error Recovery
- **Stuck Detection**: Recognizes when making no progress
- **Alternative Seeking**: Finds different approaches
- **Timeout Handling**: Prevents infinite loops
- **State Resetting**: Returns to known good states

## 📊 Performance Optimizations

### Efficient Scanning
- **Cooldown Systems**: Prevents excessive calculations
- **Range Limiting**: Only searches reasonable distances
- **State Caching**: Remembers previous decisions
- **Early Termination**: Stops searching when good option found

### Memory Management
- **Concurrent Safe**: Thread-safe player state tracking
- **Automatic Cleanup**: Removes inactive player states
- **Minimal Allocation**: Reuses objects when possible
- **Garbage Collection Friendly**: Avoids unnecessary object creation

## 🔧 Configuration Constants

```java
SEARCH_RADIUS = 16          // How far to look for trees
COLLECTION_RADIUS = 8       // How far to look for items  
REACH_DISTANCE = 4.5        // Mining reach limit
CHOP_COOLDOWN = 20          // Ticks between mining attempts
MAX_COLLECTION_ATTEMPTS = 10 // Retry limit for item collection
LEAF_CLEAR_RADIUS = 3       // Range for leaf detection
```

## 🎯 Future AI Improvements

### Planned Enhancements
- **Multi-Tree Planning**: Optimize harvesting order for multiple trees
- **Tool Durability Awareness**: Switch tools or find repair options
- **Biome Adaptation**: Adjust behavior for different environments
- **Player Preference Learning**: Adapt to user's harvesting style
- **Collaborative AI**: Multiple AI instances working together

### Advanced Features
- **Terrain Analysis**: Better understanding of landscape
- **Risk Assessment**: Avoid dangerous situations (lava, mobs)
- **Resource Management**: Optimize inventory usage
- **Time-of-Day Awareness**: Adjust behavior for day/night cycle 