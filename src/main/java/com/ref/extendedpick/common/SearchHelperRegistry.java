package com.ref.extendedpick.common;

import com.ref.extendedpick.api.ISearchHelper;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class SearchHelperRegistry {

  private static final SearchHelperRegistry INSTANCE = new SearchHelperRegistry();

  private final IdentityHashMap<Item, ISearchHelper> registry = new IdentityHashMap<>();

  private ISearchHelper defaultHelper;

  private SearchHelperRegistry() {}

  public static SearchHelperRegistry getInstance() {
    return INSTANCE;
  }

  public void register(@NotNull Item item, @NotNull ISearchHelper helper) {
    if (registry.containsKey(item)) {
      throw new IllegalStateException("Item already registered!");
    }

    registry.put(item, helper);
  }

  public void unregister(@NotNull Item item) {
    registry.remove(item);
  }

  public ISearchHelper getHelper(@NotNull Item item) {
    return registry.getOrDefault(item, this.defaultHelper);
  }

  public boolean isRegistered(@NotNull Item item) {
    return registry.containsKey(item);
  }

  public Map<Item, ISearchHelper> getRegistryView() {
    return Collections.unmodifiableMap(registry);
  }

  public void clear() {
    registry.clear();
  }

  public void setDefaultHelper(@NotNull ISearchHelper newDefaultHelper) {
    this.defaultHelper = newDefaultHelper;
  }

  /**
   * A default implementation of {@link ISearchHelper} that provides a detailed, score-based
   * similarity check, considering both item type and NBT data.
   *
   * <p>The scoring logic is as follows:
   *
   * <ul>
   *   <li>Starts with a base score if item types match.
   *   <li>Applies significant bonuses for exact NBT key-value matches.
   *   <li>Applies smaller scores for NBT keys that match but whose values differ.
   *   <li>Applies penalties if the candidate is missing NBT keys required by the target.
   *   <li>Handles nested {@link CompoundTag}s recursively.
   *   <li>Calculates proximity scores for {@link NumericTag}s (closer values get higher scores).
   *   <li>Scores {@link ListTag}s based on the number of overlapping elements.
   * </ul>
   */
  public enum DefaultSearchHelper implements ISearchHelper {
    INSTANCE;

    /** Base score for matching item types. All NBT scores are added to this. */
    private static final int SCORE_ITEM_TYPE_MATCH = 5;

    /** Bonus for an exact match of an NBT key and its value. */
    private static final int BONUS_NBT_EXACT_MATCH = 10;

    /** Base score when an NBT key matches, but the value does not (for non-specialized types). */
    private static final int SCORE_NBT_KEY_MATCH = 1;

    /** Penalty when the candidate item is missing an NBT key that the target item requires. */
    private static final int PENALTY_NBT_KEY_MISSING = -5;

    /** Multiplier for each overlapping element in a ListTag. */
    private static final int MULTIPLIER_NBT_LIST_OVERLAP = 2;

    /**
     * Base value used for calculating similarity of numeric NBT tags. The score is `BASE / (1 +
     * difference)`.
     */
    private static final double BASE_SCORE_NBT_NUMERIC_PROXIMITY = 10.0;

    @Override
    public void register() {
      SearchHelperRegistry.getInstance().setDefaultHelper(this);
    }

    @Override
    public int getMatchScore(@NotNull ItemStack target, @NotNull ItemStack candidate) {
      if (!ItemStack.isSameItem(candidate, target)) {
        return ISearchHelper.Failed;
      }

      if (!target.hasTag()) {
        return ISearchHelper.Success;
      }

      if (!candidate.hasTag()) {
        return SCORE_ITEM_TYPE_MATCH;
      }

      if (ItemStack.isSameItemSameTags(candidate, target)) {
        return ISearchHelper.Success;
      }

      int totalScore = SCORE_ITEM_TYPE_MATCH;

      totalScore += calculateNbtSimilarity(candidate.getTag(), target.getTag());

      return totalScore;
    }

    /**
     * Recursively calculates the similarity score between two CompoundTags. It iterates through the
     * keys of the target NBT and scores the candidate based on them.
     *
     * @param candidateNbt The NBT data of the candidate item.
     * @param targetNbt The NBT data of the target item (the criteria).
     * @return The calculated NBT similarity score.
     */
    private int calculateNbtSimilarity(CompoundTag candidateNbt, CompoundTag targetNbt) {
      int score = 0;
      for (String key : targetNbt.getAllKeys()) {
        if (candidateNbt.contains(key)) {
          Tag candidateTag = candidateNbt.get(key);
          Tag targetTag = targetNbt.get(key);

          if (Objects.equals(candidateTag, targetTag)) {
            score += BONUS_NBT_EXACT_MATCH;
          } else {
            score += scoreBasedOnType(candidateTag, targetTag);
          }
        } else {
          score += PENALTY_NBT_KEY_MISSING;
        }
      }
      return score;
    }

    /**
     * Calculates a similarity score based on the specific type of the NBT tags. This allows for
     * more nuanced comparisons than simple equality checks.
     *
     * @param candidateTag The NBT tag from the candidate item.
     * @param targetTag The NBT tag from the target item.
     * @return A score representing the similarity of the two tags.
     */
    private int scoreBasedOnType(Tag candidateTag, Tag targetTag) {
      if (candidateTag instanceof CompoundTag cTag && targetTag instanceof CompoundTag tTag) {
        return calculateNbtSimilarity(cTag, tTag);
      }

      if (candidateTag instanceof NumericTag cNum && targetTag instanceof NumericTag tNum) {
        double diff = Math.abs(cNum.getAsDouble() - tNum.getAsDouble());
        return (int) (BASE_SCORE_NBT_NUMERIC_PROXIMITY / (1.0 + diff));
      }

      if (candidateTag instanceof ListTag cList && targetTag instanceof ListTag tList) {
        if (cList.isEmpty() && tList.isEmpty()) {
          return BONUS_NBT_EXACT_MATCH;
        }
        if (cList.isEmpty() || tList.isEmpty()) {
          return SCORE_NBT_KEY_MATCH;
        }

        Set<Tag> candidateSet = new HashSet<>(cList);
        Set<Tag> targetSet = new HashSet<>(tList);

        // Find the intersection of the two sets.
        candidateSet.retainAll(targetSet);
        return candidateSet.size() * MULTIPLIER_NBT_LIST_OVERLAP;
      }

      return SCORE_NBT_KEY_MATCH;
    }
  }
}
