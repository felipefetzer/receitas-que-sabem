package com.receitasquesabem.shoppinglist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.receitasquesabem.activation.Activation;
import com.receitasquesabem.activation.ActivationDtos.ShoppingGroupResponse;
import com.receitasquesabem.activation.ActivationDtos.ShoppingItemResponse;
import com.receitasquesabem.activation.ActivationRepository;
import com.receitasquesabem.auth.CurrentUser;
import com.receitasquesabem.user.User;

/**
 * A lista de compras não tem tabela própria: é derivada dos ingredientes
 * marcados como "não tenho" nas ativações do utilizador (R17).
 * Por isso nunca pode ficar dessincronizada.
 */
@RestController
@RequestMapping("/api/shopping-list")
public class ShoppingListController {

    private final ActivationRepository activations;
    private final CurrentUser currentUser;

    public ShoppingListController(ActivationRepository activations, CurrentUser currentUser) {
        this.activations = activations;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ShoppingGroupResponse> list() {
        User me = currentUser.get();
        List<ShoppingGroupResponse> groups = new ArrayList<>();

        for (Activation activation : activations.findByUserId(me.getId())) {
            List<ShoppingItemResponse> items = new ArrayList<>(activation.getIngredients().stream()
                    .filter(ai -> !ai.isAvailable())
                    .sorted(Comparator.comparingInt(ai -> ai.getIngredient().getPosition()))
                    .map(ai -> new ShoppingItemResponse(
                            activation.getId(),
                            ai.getIngredient().getId(),
                            activation.getRecipe().getName(),
                            ai.getIngredient().getQuantity(),
                            ai.getIngredient().getUnit(),
                            ai.getIngredient().getItem()))
                    .toList());

            if (!items.isEmpty()) {
                groups.add(new ShoppingGroupResponse(activation.getId(),
                        activation.getRecipe().getName(), items));
            }
        }
        return groups;
    }
}
