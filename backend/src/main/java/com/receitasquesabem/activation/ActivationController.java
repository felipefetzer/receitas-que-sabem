package com.receitasquesabem.activation;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.receitasquesabem.activation.ActivationDtos.ActivateRequest;
import com.receitasquesabem.activation.ActivationDtos.ActivationResponse;
import com.receitasquesabem.activation.ActivationDtos.AvailabilityRequest;

@RestController
@RequestMapping("/api/activations")
public class ActivationController {

    private final ActivationService service;

    public ActivationController(ActivationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivationResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivationResponse activate(@RequestBody ActivateRequest request) {
        return service.activate(request.recipeId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }

    @PatchMapping("/{id}/ingredients/{ingredientId}")
    public ActivationResponse setAvailability(@PathVariable Long id,
                                              @PathVariable Long ingredientId,
                                              @RequestBody AvailabilityRequest request) {
        return service.setAvailability(id, ingredientId, request.available());
    }
}
