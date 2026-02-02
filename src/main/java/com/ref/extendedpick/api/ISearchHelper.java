package com.ref.extendedpick.api;

import com.ref.extendedpick.common.SearchHelperRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Defines a contract for helpers that calculate the similarity between two ItemStacks. This is
 * useful for fuzzy search, recipe matching, or item request systems.
 */
public interface ISearchHelper {

  /** A score representing a perfect match. This should be the highest possible score. */
  int Success = Integer.MAX_VALUE;

  /** A score representing a complete mismatch, typically when item types are different. */
  int Failed = 0;

  /**
   * Calculates a similarity score between a candidate ItemStack and a target ItemStack. A higher
   * score indicates a better match.
   *
   * @param target The ItemStack being searched for (the criteria).
   * @param candidate The ItemStack being checked against the target (e.g., from a player's
   *     inventory).
   * @return An integer score representing the similarity. A perfect match should return {@link
   *     #Success}, and a complete mismatch should return {@link #Failed} or less.
   */
  int getMatchScore(@NotNull ItemStack target, @NotNull ItemStack candidate);

  void register();

  default void register(@NotNull Item item) {
    SearchHelperRegistry.getInstance().register(item, this);
  }
}
