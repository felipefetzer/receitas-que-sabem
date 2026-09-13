package com.receitasquesabem.activation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.receitasquesabem.activation.ActivationDtos.ActivationIngredientResponse;
import com.receitasquesabem.activation.ActivationDtos.ActivationResponse;
import com.receitasquesabem.auth.CurrentUser;
import com.receitasquesabem.common.ApiExceptions.ConflictException;
import com.receitasquesabem.common.ApiExceptions.ForbiddenException;
import com.receitasquesabem.common.ApiExceptions.NotFoundException;
import com.receitasquesabem.recipe.Ingredient;
import com.receitasquesabem.recipe.Recipe;
import com.receitasquesabem.recipe.RecipeRepository;
import com.receitasquesabem.user.User;

@Service
public class ActivationService {

    /** R13 — limite de receitas ativas em simultâneo. */
    public static final int MAX_ACTIVE = 5;

    private final ActivationRepository activations;
    private final RecipeRepository recipes;
    private final CurrentUser currentUser;

    public ActivationService(ActivationRepository activations, RecipeRepository recipes, CurrentUser currentUser) {
        this.activations = activations;
        this.recipes = recipes;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<ActivationResponse> list() {
        User me = currentUser.get();
        return activations.findByUserId(me.getId()).stream()
                .map(ActivationService::toResponse)
                .toList();
    }

    @Transactional
    public ActivationResponse activate(Long recipeId) {
        User me = currentUser.get();

        if (activations.findByUserIdAndRecipeId(me.getId(), recipeId).isPresent()) {
            throw new ConflictException("Esta receita já está ativa.");
        }
        if (activations.countByUserId(me.getId()) >= MAX_ACTIVE) {
            throw new ConflictException("Já tem " + MAX_ACTIVE + " receitas ativas. Desative uma primeiro.");
        }

        Recipe recipe = recipes.findById(recipeId)
                .orElseThrow(() -> new NotFoundException("Receita não encontrada."));

        Activation activation = new Activation(me, recipe);
        // R14: assume-se que falta tudo até o utilizador dizer o contrário.
        for (Ingredient ingredient : recipe.getIngredients()) {
            activation.getIngredients().add(new ActivationIngredient(activation, ingredient, false));
        }
        return toResponse(activations.save(activation));
    }

    @Transactional
    public void deactivate(Long activationId) {
        Activation activation = loadMine(activationId);
        activations.delete(activation);
    }

    @Transactional
    public ActivationResponse setAvailability(Long activationId, Long ingredientId, boolean available) {
        Activation activation = loadMine(activationId);
        ActivationIngredient target = activation.getIngredients().stream()
                .filter(ai -> ai.getIngredient().getId().equals(ingredientId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Ingrediente não faz parte desta ativação."));
        target.setAvailable(available);
        return toResponse(activations.save(activation));
    }

    @Transactional(readOnly = true)
    public Activation loadMine(Long activationId) {
        User me = currentUser.get();
        Activation activation = activations.findById(activationId)
                .orElseThrow(() -> new NotFoundException("Ativação não encontrada."));
        if (!activation.getUser().getId().equals(me.getId())) {
            throw new ForbiddenException("Esta ativação não é sua.");
        }
        return activation;
    }

    static ActivationResponse toResponse(Activation activation) {
        List<ActivationIngredientResponse> ingredients = new ArrayList<>(
                activation.getIngredients().stream()
                        .map(ai -> new ActivationIngredientResponse(
                                ai.getIngredient().getId(),
                                ai.getIngredient().getPosition(),
                                ai.getIngredient().getQuantity(),
                                ai.getIngredient().getUnit(),
                                ai.getIngredient().getItem(),
                                ai.isAvailable()))
                        .toList());
        ingredients.sort(Comparator.comparingInt(ActivationIngredientResponse::position));

        return new ActivationResponse(
                activation.getId(),
                activation.getRecipe().getId(),
                activation.getRecipe().getName(),
                activation.getRecipe().getAuthor().getUsername(),
                ingredients);
    }
}
