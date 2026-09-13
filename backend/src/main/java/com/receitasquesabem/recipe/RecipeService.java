package com.receitasquesabem.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.receitasquesabem.activation.Activation;
import com.receitasquesabem.activation.ActivationRepository;
import com.receitasquesabem.auth.CurrentUser;
import com.receitasquesabem.common.ApiExceptions.ConflictException;
import com.receitasquesabem.common.ApiExceptions.ForbiddenException;
import com.receitasquesabem.common.ApiExceptions.NotFoundException;
import com.receitasquesabem.recipe.RecipeDtos.IngredientRequest;
import com.receitasquesabem.recipe.RecipeDtos.IngredientResponse;
import com.receitasquesabem.recipe.RecipeDtos.RecipeDetailResponse;
import com.receitasquesabem.recipe.RecipeDtos.RecipeRequest;
import com.receitasquesabem.recipe.RecipeDtos.RecipeSummaryResponse;
import com.receitasquesabem.recipe.RecipeDtos.SectionRequest;
import com.receitasquesabem.recipe.RecipeDtos.SectionResponse;
import com.receitasquesabem.recipe.RecipeDtos.StepRequest;
import com.receitasquesabem.recipe.RecipeDtos.StepResponse;
import com.receitasquesabem.user.User;

@Service
public class RecipeService {

    private final RecipeRepository recipes;
    private final ActivationRepository activations;
    private final CurrentUser currentUser;

    public RecipeService(RecipeRepository recipes, ActivationRepository activations, CurrentUser currentUser) {
        this.recipes = recipes;
        this.activations = activations;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<RecipeSummaryResponse> list(boolean onlyMine) {
        User me = currentUser.get();
        List<Recipe> found = onlyMine ? recipes.findMineWithAuthor(me.getId()) : recipes.findAllWithAuthor();

        Map<Long, Activation> active = activations.findByUserId(me.getId()).stream()
                .collect(java.util.stream.Collectors.toMap(a -> a.getRecipe().getId(), a -> a));

        List<RecipeSummaryResponse> result = new ArrayList<>();
        for (Recipe r : found) {
            result.add(new RecipeSummaryResponse(
                    r.getId(),
                    r.getName(),
                    r.getAuthor().getUsername(),
                    r.getAuthor().getId().equals(me.getId()),
                    active.containsKey(r.getId()),
                    r.getIngredients().size()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public RecipeDetailResponse get(Long id) {
        User me = currentUser.get();
        Recipe recipe = load(id);
        Optional<Activation> activation = activations.findByUserIdAndRecipeId(me.getId(), id);
        return toDetail(recipe, me, activation.map(Activation::getId).orElse(null));
    }

    @Transactional
    public RecipeDetailResponse create(RecipeRequest request) {
        User me = currentUser.get();
        if (recipes.existsByAuthorIdAndNameIgnoreCase(me.getId(), request.name().trim())) {
            throw new ConflictException("Já tem uma receita com esse nome.");
        }
        Recipe recipe = new Recipe(me, request.name().trim(), request.notes());
        fill(recipe, request);
        return toDetail(recipes.save(recipe), me, null);
    }

    @Transactional
    public RecipeDetailResponse update(Long id, RecipeRequest request) {
        User me = currentUser.get();
        Recipe recipe = load(id);
        requireAuthor(recipe, me);

        if (recipes.existsByAuthorIdAndNameIgnoreCaseAndIdNot(me.getId(), request.name().trim(), id)) {
            throw new ConflictException("Já tem outra receita com esse nome.");
        }

        recipe.setName(request.name().trim());
        recipe.setNotes(request.notes());
        // Reescrever o agregado inteiro é mais simples e mais seguro do que
        // tentar casar item a item o que mudou. orphanRemoval trata do resto.
        recipe.getIngredients().clear();
        recipe.getSections().clear();
        recipe.getUtensils().clear();
        fill(recipe, request);

        Long activationId = activations.findByUserIdAndRecipeId(me.getId(), id)
                .map(Activation::getId).orElse(null);
        return toDetail(recipes.save(recipe), me, activationId);
    }

    @Transactional
    public void delete(Long id) {
        User me = currentUser.get();
        Recipe recipe = load(id);
        requireAuthor(recipe, me);
        recipes.delete(recipe);
    }

    // ---------- auxiliares ----------

    private Recipe load(Long id) {
        return recipes.findById(id)
                .orElseThrow(() -> new NotFoundException("Receita não encontrada."));
    }

    private void requireAuthor(Recipe recipe, User me) {
        if (!recipe.getAuthor().getId().equals(me.getId())) {
            throw new ForbiddenException("Esta receita não é sua.");
        }
    }

    private void fill(Recipe recipe, RecipeRequest request) {
        List<IngredientRequest> ingredients = request.ingredients() == null ? List.of() : request.ingredients();
        for (int i = 0; i < ingredients.size(); i++) {
            IngredientRequest in = ingredients.get(i);
            recipe.getIngredients().add(new Ingredient(recipe, i, in.quantity(), in.unit(), in.item().trim()));
        }

        List<SectionRequest> sections = request.sections() == null ? List.of() : request.sections();
        for (int s = 0; s < sections.size(); s++) {
            SectionRequest sr = sections.get(s);
            PreparationSection section = new PreparationSection(recipe, s, sr.title().trim());
            List<StepRequest> steps = sr.steps() == null ? List.of() : sr.steps();
            for (int p = 0; p < steps.size(); p++) {
                StepRequest st = steps.get(p);
                section.getSteps().add(new PreparationStep(section, p, st.instruction().trim(), st.durationMinutes()));
            }
            recipe.getSections().add(section);
        }

        List<String> utensils = request.utensils() == null ? List.of() : request.utensils();
        for (String name : utensils) {
            if (name != null && !name.isBlank()) {
                recipe.getUtensils().add(new Utensil(recipe, name.trim()));
            }
        }
    }

    private RecipeDetailResponse toDetail(Recipe recipe, User me, Long activationId) {
        List<IngredientResponse> ingredients = recipe.getIngredients().stream()
                .map(i -> new IngredientResponse(i.getId(), i.getPosition(), i.getQuantity(), i.getUnit(), i.getItem()))
                .toList();

        List<SectionResponse> sections = recipe.getSections().stream()
                .map(RecipeService::toSection)
                .toList();

        return new RecipeDetailResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getNotes(),
                recipe.getAuthor().getUsername(),
                recipe.getAuthor().getId().equals(me.getId()),
                activationId,
                ingredients,
                sections,
                recipe.getUtensils().stream().map(Utensil::getName).toList());
    }

    private static SectionResponse toSection(PreparationSection section) {
        int total = 0;
        boolean partial = false;
        List<StepResponse> steps = new ArrayList<>();
        for (PreparationStep step : section.getSteps()) {
            if (step.getDurationMinutes() != null) {
                total += step.getDurationMinutes();
            } else {
                partial = true;   // R10
            }
            steps.add(new StepResponse(step.getId(), step.getPosition(),
                    step.getInstruction(), step.getDurationMinutes()));
        }
        return new SectionResponse(section.getId(), section.getPosition(), section.getTitle(),
                steps, total, partial);
    }
}
