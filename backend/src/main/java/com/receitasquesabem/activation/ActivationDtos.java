package com.receitasquesabem.activation;

import java.util.List;

public final class ActivationDtos {

    private ActivationDtos() {
    }

    public record ActivateRequest(Long recipeId) {
    }

    public record AvailabilityRequest(boolean available) {
    }

    public record ActivationIngredientResponse(Long ingredientId, int position, String quantity,
                                               String unit, String item, boolean available) {
    }

    public record ActivationResponse(Long id, Long recipeId, String recipeName, String authorUsername,
                                     List<ActivationIngredientResponse> ingredients) {
    }

    /** Um item em falta, já com a receita de onde veio (R18). */
    public record ShoppingItemResponse(Long activationId, Long ingredientId, String recipeName,
                                       String quantity, String unit, String item) {
    }

    public record ShoppingGroupResponse(Long activationId, String recipeName,
                                        List<ShoppingItemResponse> items) {
    }
}
