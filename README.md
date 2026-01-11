# BlonAddon

A Fabric mod for Meteor Client that adds powerful command block utilities.

## Features

### SchematicPlacer Module
Places Litematica schematics using command blocks via the command block core system.

**Recent Fixes:**
- **Fixed Structure Placement**: Replaced volume optimization with individual `setblock` commands to ensure exact schematic structure preservation (including holes and complex shapes)
- **Fixed Invalid Packet Kicks**: Implemented proper block state string generation using Minecraft registry methods instead of unreliable string manipulation
- **Added Region Origin Support**: Now correctly handles schematic region origins for accurate positioning

**Usage:**
1. Create a command block core: `.core` (default 16x16x32)
2. Place your `.litematica` schematic file in `.minecraft/schematics/`
3. Enable SchematicPlacer module and set the schematic file name (without extension)
4. Adjust placement mode and offset as needed
5. The module will generate individual setblock commands sent via your command block core

**Settings:**
- `schematic-file`: Name of the schematic file (without .litematica extension)
- `placement-mode`: Relative (to player position) or Absolute (world origin)
- `placement-offset`: Additional offset from placement position
- `packets-per-tick`: Number of commands to send per tick (default: 50000, reduce if experiencing issues)
- `core-mode`: Command block execution mode
- `ignore-air`: Skip air blocks (recommended)
- `place-waterlogged`: Include waterlogged blocks

### Command Block Core System
- **CoreCommand (.core)**: Creates a 3D grid of command blocks for high-throughput command execution
- **CoreUpdater**: Distributes commands across all command blocks in the core
- **Multiple Modes**: AUTO, REDSTONE, SEQUENCE, SINGLE execution modes

### Other Modules
- ChatBubble: Custom chat display
- ForceGrab: Enhanced entity grabbing
- ImageEggs: Image display using spawn eggs
- LegitKillAura: Legitimate kill aura
- NoParticleSpam: Reduces particle spam
- ParticleAuraModule: Particle-based aura effects
- PlayerRotatorModule: Player rotation utilities
- SongPlayer: Audio playback
- TotemBypass: Totem bypass utilities

### Commands
- `.core [width] [length] [height]`: Create command block core (default 16x16x32)
- `.core decore`: Remove command block core
- `.core refresh`: Refresh command block core

## Installation

1. Install Fabric Loader and Meteor Client
2. Place the mod JAR in your `.minecraft/mods/` folder
3. Launch Minecraft

# BlonAddon

A Fabric mod for Meteor Client that adds powerful command block utilities.

## Features

### SchematicPlacer Module
Places Litematica schematics using command blocks via the command block core system.

**Recent Fixes:**
- **Fixed Structure Placement**: Replaced volume optimization with individual `setblock` commands to ensure exact schematic structure preservation (including holes and complex shapes)
- **Fixed Invalid Packet Kicks**: Implemented proper block state string generation using Minecraft registry methods instead of unreliable string manipulation
- **Added Region Origin Support**: Now correctly handles schematic region origins for accurate positioning

**Usage:**
1. Create a command block core: `.core` (default 16x16x32)
2. Place your `.litematica` schematic file in `.minecraft/schematics/`
3. Enable SchematicPlacer module and set the schematic file name (without extension)
4. Adjust placement mode and offset as needed
5. The module will generate individual setblock commands sent via your command block core

**Settings:**
- `schematic-file`: Name of the schematic file (without .litematica extension)
- `placement-mode`: Relative (to player position) or Absolute (world origin)
- `placement-offset`: Additional offset from placement position
- `packets-per-tick`: Number of commands to send per tick (default: 50000, reduce if experiencing issues)
- `core-mode`: Command block execution mode
- `ignore-air`: Skip air blocks (recommended)
- `place-waterlogged`: Include waterlogged blocks

### Command Block Core System
- **CoreCommand (.core)**: Creates a 3D grid of command blocks for high-throughput command execution
- **CoreUpdater**: Distributes commands across all command blocks in the core
- **Multiple Modes**: AUTO, REDSTONE, SEQUENCE, SINGLE execution modes

### Other Modules
- ChatBubble: Custom chat display
- ForceGrab: Enhanced entity grabbing
- ImageEggs: Image display using spawn eggs
- LegitKillAura: Legitimate kill aura
- NoParticleSpam: Reduces particle spam
- ParticleAuraModule: Particle-based aura effects
- PlayerRotatorModule: Player rotation utilities
- SongPlayer: Audio playback
- TotemBypass: Totem bypass utilities

### Commands
- `.core [width] [length] [height]`: Create command block core (default 16x16x32)
- `.core decore`: Remove command block core
- `.core refresh`: Refresh command block core

## Installation

1. Install Fabric Loader and Meteor Client
2. Place the mod JAR in your `.minecraft/mods/` folder
3. Launch Minecraft

## Join the Discord to give suggestions:

https://discord.gg/5PFjMYh6SG

## Dependencies

- Fabric API
- Meteor Client
- Litematica (runtime dependency for schematic loading)

## License

This project is provided as-is for educational purposes.


## Dependencies

- Fabric API
- Meteor Client
- Litematica (runtime dependency for schematic loading)

## License

This project is provided as-is for educational purposes.
