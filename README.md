<div >

**English** | [简体中文](README_zh_CN.md)

</div>

# ExtendedPick

**ExtendedPick** is a Minecraft Forge mod that significantly upgrades the standard "Pick Block" (Middle-Click) functionality. It allows players to find items in their inventory more intelligently, even if the item is buried inside a backpack or has slightly different NBT data.

## Features

### 1. Smart / Fuzzy Matching
The vanilla Pick Block function requires an exact item match. ExtendedPick introduces `ISearchHelper`, allowing for "best match" scoring.
- If you try to pick a block and don't have the *exact* item, the mod searches your inventory for the most similar item based on registered logic.
- It prioritizes the Hotbar but will swap items from the main inventory if necessary.

### 2. Deep Search (Container Support)
If the item isn't found in your main inventory, ExtendedPick can search **inside** item containers (like Backpacks, Shulker Boxes, or Bundles) that you are carrying.
- This is handled via the `IDeepSearchProvider` API.
- **Forge Capability Support**: Items implementing `ForgeCapabilities.ITEM_HANDLER` are supported automatically by default.
- If the item is found inside a container, the mod communicates with the server to extract it automatically (configurable).
### 3. Advanced Creative Entity Picking
Enhances Creative Mode picking for entities:
- **Ctrl + Pick Block (Middle Click)** on an Entity will copy the Entity with its full NBT data (excluding position/UUID), allowing you to place down an exact copy of that mob.

### 4. JEI Recipe Transfer Integration
Extends the "Deep Search" logic to **Just Enough Items (JEI)**.
- When clicking the "+" button to transfer a recipe into a crafting grid, ExtendedPick will automatically search for and extract required ingredients from inside your carried containers (Backpacks, etc.) if they are missing from your main inventory.

### 5. Configurable
The mod allows customization via `ExtendedPickClientConfig`:
- Toggle Deep Search.
- Enable/Disable debug logging for picking actions.
- Toggle Creative NBT picking behavior.

## For Developers (API)

ExtendedPick provides an API for other mods to integrate their items and containers.

### `ISearchHelper`
Implement this interface to define how two items should be compared (Fuzzy Search).
```java
public class MyISearchHelper implements ISearchHelper {
    @Override
    int getMatchScore(@NotNull ItemStack target, @NotNull ItemStack candidate){
       //Logic to Match Score
    }
}
```

### `IDeepSearchProvider<T>`
Implement this interface to allow ExtendedPick to look inside your mod's containers. `T` represents your internal index type (e.g., `Integer` for slot ID).

```java
public class MyBackpackProvider implements IDeepSearchProvider<Integer> {
    @Override
    public void forEachItem(@NotNull ItemStack container, @NotNull Predicate<IndexedStack<Integer>> action) {
        // Logic to iterate container contents. Return true/false in predicate to continue/stop.
    }

    @NotNull
    @Override
    public ItemStack extract(@NotNull ServerPlayer player, @NotNull ItemStack container, @NotNull Integer index, int amount, boolean simulate) {
        // Logic to remove item from container and return it
        // amount: 0 (MAX_STACK_SIZE) or specific count
        // simulate: if true, do not actually modify the stack
    }
}
```
