package com.receitasquesabem.activation;

import com.receitasquesabem.recipe.Ingredient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** "Tenho" ou "não tenho" este ingrediente, nesta ativação. */
@Entity
@Table(name = "activation_ingredients")
public class ActivationIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activation_id")
    private Activation activation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @Column(nullable = false)
    private boolean available;

    protected ActivationIngredient() {
    }

    public ActivationIngredient(Activation activation, Ingredient ingredient, boolean available) {
        this.activation = activation;
        this.ingredient = ingredient;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public Activation getActivation() {
        return activation;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
