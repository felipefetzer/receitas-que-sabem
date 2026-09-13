package com.receitasquesabem.recipe;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.receitasquesabem.recipe.RecipeDtos.RecipeDetailResponse;
import com.receitasquesabem.recipe.RecipeDtos.RecipeRequest;
import com.receitasquesabem.recipe.RecipeDtos.RecipeSummaryResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService service;

    public RecipeController(RecipeService service) {
        this.service = service;
    }

    @GetMapping
    public List<RecipeSummaryResponse> list(@RequestParam(defaultValue = "all") String scope) {
        return service.list("mine".equalsIgnoreCase(scope));
    }

    @GetMapping("/{id}")
    public RecipeDetailResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecipeDetailResponse create(@Valid @RequestBody RecipeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public RecipeDetailResponse update(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
