package com.receitasquesabem.recipe;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Todos os contratos da API de receitas, num sítio só. */
public final class RecipeDtos {

    private RecipeDtos() {
    }

    // ---------- entrada ----------

    public record IngredientRequest(
            String quantity,
            String unit,
            @NotBlank @Size(max = 150) String item) {
    }

    public record StepRequest(
            @NotBlank String instruction,
            @Min(0) Integer durationMinutes) {
    }

    public record SectionRequest(
            @NotBlank @Size(max = 120) String title,
            @Valid List<StepRequest> steps) {
    }

    public record RecipeRequest(
            @NotBlank @Size(max = 120) String name,
            String notes,
            @Valid List<IngredientRequest> ingredients,
            @Valid List<SectionRequest> sections,
            List<String> utensils) {
    }

    // ---------- saída ----------

    public record IngredientResponse(Long id, int position, String quantity, String unit, String item) {
    }

    public record StepResponse(Long id, int position, String instruction, Integer durationMinutes) {
    }

    /**
     * totalMinutes é a soma dos passos com tempo (R8).
     * partial indica que nem todos tinham tempo, para o frontend
     * escrever "aproximadamente" (R10).
     */
    public record SectionResponse(Long id, int position, String title, List<StepResponse> steps,
                                  int totalMinutes, boolean partial) {
    }

    public record RecipeSummaryResponse(Long id, String name, String authorUsername,
                                        boolean mine, boolean active, int ingredientCount) {
    }

    public record RecipeDetailResponse(Long id, String name, String notes, String authorUsername,
                                       boolean mine, Long activationId,
                                       List<IngredientResponse> ingredients,
                                       List<SectionResponse> sections,
                                       List<String> utensils) {
    }
}
