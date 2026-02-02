# ExtendedPick (中文说明)

**ExtendedPick** 是一个 Minecraft Forge模组，它极大地增强了原版“选取方块”（Pick Block / 中键选取）的功能。即使物品藏在背包中或 NBT 数据略有不同，它也能帮助玩家智能地在物品栏中找到目标物品。

## 主要功能

### 1. 智能/模糊匹配 (Smart Matching)
原版的选取方块功能通常需要物品完全一致才能选中。ExtendedPick 引入了 `ISearchHelper` 机制，支持评分制的“最佳匹配”。
- 当你尝试选取一个方块，且身上没有完全一样的物品时，模组会搜索你的物品栏，寻找最相似的替代品。
- 优先检索快捷栏，如果物品在背包主栏中，会自动切换到快捷栏。

### 2. 深度搜索 (Deep Search)
如果物品不在你的主物品栏中，ExtendedPick 可以搜索你携带的**容器物品内部**（例如背包、潜影盒或收纳袋）。
- 这是通过 `IDeepSearchProvider` API 实现的。
- 如果在容器内找到了目标物品，模组会向服务器发送数据包，自动将其提取出来（此功能可配置）。

### 3. 创造模式实体增强
增强了创造模式下对实体的选取功能：
- **Ctrl + 中键** 点击实体：将复制该实体及其完整的 NBT 数据（排除坐标/UUID），让你能生成一个属性完全相同的生物副本。

### 4. 高度可配置
可以通过 `ExtendedPickClientConfig` 进行自定义：
- 开启/关闭深度搜索。
- 开启/关闭选取操作的调试日志 (Debug Log)。
- 切换创造模式 NBT 选取行为。

## 开发者接口 (API)

ExtendedPick 提供了简单的 API，允许其他模组兼容其物品和容器。

### `ISearchHelper`
实现此接口以定义两个物品之间的相似度比较逻辑（模糊搜索）。
```java
public class MyISearchHelper implements ISearchHelper {
    @Override
    int getMatchScore(@NotNull ItemStack target, @NotNull ItemStack candidate){
        //打分逻辑
    }
}
```

### `IDeepSearchProvider`
实现此接口以允许 ExtendedPick 搜索你模组中的容器（如背包）。
```java
public class MyBackpackProvider implements IDeepSearchProvider {
    @Override
    public void forEachItem(ItemStack container, Consumer<IndexedStack> action) {
        // 遍历容器内容的逻辑
    }

    @Override
    public ItemStack extract(ServerPlayer player, ItemStack container, int index) {
        // 从容器中提取物品并交给玩家的逻辑
    }
}
```
