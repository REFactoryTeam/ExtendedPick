package com.ref.extendedpick.mixin.aea;

import appeng.menu.me.items.CraftingTermMenu;
import com.ref.aea.integration.aea.advancedterminal.AdvancedTerminalMenu;
import com.ref.extendedpick.ExtendedPick;
import com.ref.extendedpick.api.IDeepSearchProvider;
import com.ref.extendedpick.api.IPlayerInventoryAccess;
import com.ref.extendedpick.config.ExtendedPickClientConfig;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
@Mixin(value = AdvancedTerminalMenu.class, remap = false)
public class AdvancedTerminalMenuMixin {

  @Inject(method = "findMissingIngredients", at = @At("RETURN"), cancellable = true)
  private void onFindMissingIngredients(
      Map<Integer, Ingredient> ingredients,
      CallbackInfoReturnable<CraftingTermMenu.MissingIngredientSlots> cir) {
    if (!(ExtendedPickClientConfig.exPick && ExtendedPickClientConfig.exPickPacket)) return;
    if (!ExtendedPick.isServerModLoaded) return;
    CraftingTermMenu.MissingIngredientSlots result = cir.getReturnValue();
    if (result.missingSlots().isEmpty()) return;

    Set<Integer> newMissingSlots = new HashSet<>(result.missingSlots());
    Set<Integer> newCraftableSlots = new HashSet<>(result.craftableSlots());

    AdvancedTerminalMenu menu = (AdvancedTerminalMenu) (Object) this;
    Player player = menu.getPlayerInventory().player;

    Map<Item, Integer> reservedDeepItems = new HashMap<>();

    Iterator<Integer> iterator = newMissingSlots.iterator();
    while (iterator.hasNext()) {
      int slot = iterator.next();
      Ingredient ingredient = ingredients.get(slot);
      boolean[] found = {false};

      IPlayerInventoryAccess.findStacks(
          player,
          (container, context) -> {
            if (found[0] || container.isEmpty()) return;
            if (!context.isCurios() && menu.isPlayerInventorySlotLocked(context.index())) return;

            IDeepSearchProvider<?> provider = IDeepSearchProvider.getProvider(container);
            if (provider != null) {
              provider.forEachItem(
                  container,
                  indexed -> {
                    if (found[0]) return false;

                    ItemStack stack = indexed.stack();
                    if (ingredient.test(stack)) {
                      int reserved = reservedDeepItems.getOrDefault(stack.getItem(), 0);
                      if (stack.getCount() > reserved) {
                        reservedDeepItems.put(stack.getItem(), reserved + 1);
                        found[0] = true;
                        return false;
                      }
                    }
                    return true;
                  });
            }
          });

      if (found[0]) {
        iterator.remove();
      }
    }

    if (newMissingSlots.size() != result.missingSlots().size()) {
      cir.setReturnValue(
          new CraftingTermMenu.MissingIngredientSlots(newMissingSlots, newCraftableSlots));
    }
  }
}
