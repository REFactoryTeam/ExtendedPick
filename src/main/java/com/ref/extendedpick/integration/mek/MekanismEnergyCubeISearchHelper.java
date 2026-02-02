package com.ref.extendedpick.integration.mek;

import com.ref.extendedpick.api.ISearchHelper;
import net.minecraft.nbt.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public enum MekanismEnergyCubeISearchHelper implements ISearchHelper {
  INSTANCE;

  final Logger LOGGER = LogManager.getLogger();

  // --- 开发时可选的开关 ---
  private static final boolean COMPARE_BY_SIMILARITY = true;

  // --- 为评分系统定义的常量 ---
  private static final int MATCH_TYPE_SCORE = 100;

  // -- 用于最大值模式 --
  private static final double MAX_MAPPED_ENERGY = 10_000_000_000_000.0; // 10 Trillion
  private static final int ENERGY_SCORE_RANGE = 1_000_000_000;

  // -- 用于相似度模式 (对数评分) --
  // 定义一个分数上限，当diff=0时，得到这个分数
  private static final int LOG_SCORE_CEILING = 1_000_000;
  // 定义一个乘数，控制分数下降的陡峭程度
  private static final int LOG_SCORE_MULTIPLIER = 100_000;

  @Override
  public void register() {
    if (!ModList.get().isLoaded("mekanism")) {
      return;
    }
    int mekcount = 0;
    for (Item item : ForgeRegistries.ITEMS.getValues()) {
      if (item instanceof mekanism.common.item.interfaces.IItemSustainedInventory) {
        this.register(item);
        mekcount++;
      }
    }
    LOGGER.debug("Dynamically registered Mekanism SearchHelper for {} items.", mekcount);
  }

  @Override
  public int getMatchScore(@NotNull ItemStack candidate, @NotNull ItemStack target) {
    if (!candidate.is(target.getItem())) {
      return -1;
    }

    if (ItemStack.isSameItemSameTags(candidate, target)) {
      return Integer.MAX_VALUE;
    }

    long candidateEnergy = getStoredEnergy(candidate);
    if (COMPARE_BY_SIMILARITY) {
      // --- 相似度模式 (使用对数评分，最终版) ---
      long targetEnergy = getStoredEnergy(target);

      long longDiff =
          (candidateEnergy > targetEnergy)
              ? (candidateEnergy - targetEnergy)
              : (targetEnergy - candidateEnergy);

      int similarityScore =
          (int) (LOG_SCORE_CEILING - Math.log10(1.0 + longDiff) * LOG_SCORE_MULTIPLIER);
      return MATCH_TYPE_SCORE + Math.max(0, similarityScore);

    } else {
      // --- 最大值模式 (安全实现) ---
      double energyRatio = Math.min(1.0, candidateEnergy / MAX_MAPPED_ENERGY);
      int energyScore = (int) (energyRatio * ENERGY_SCORE_RANGE);
      return MATCH_TYPE_SCORE + energyScore;
    }
  }

  private long getStoredEnergy(ItemStack stack) {
    if (!stack.hasTag()) return 0;
    CompoundTag rootTag = stack.getTag();
    if (rootTag == null || !rootTag.contains("mekData", Tag.TAG_COMPOUND)) return 0;
    CompoundTag mekData = rootTag.getCompound("mekData");
    if (!mekData.contains("EnergyContainers", Tag.TAG_LIST)) return 0;
    ListTag energyContainers = mekData.getList("EnergyContainers", Tag.TAG_COMPOUND);
    if (energyContainers.isEmpty()) return 0;
    CompoundTag firstContainer = energyContainers.getCompound(0);
    if (firstContainer.contains("stored")) {
      Tag storedTag = firstContainer.get("stored");

      if (storedTag instanceof NumericTag numericTag) {
        return (long) numericTag.getAsDouble();
      }

      if (storedTag instanceof StringTag stringTag) {
        try {
          return (long) Double.parseDouble(stringTag.getAsString());
        } catch (NumberFormatException e) {
          return 0;
        }
      }
    }
    return 0;
  }
}
