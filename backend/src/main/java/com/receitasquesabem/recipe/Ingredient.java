package com.receitasquesabem.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ingredients")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(nullable = false)
    private int position;

    @Column(length = 50)
    private String quantity;

    @Column(length = 50)
    private String unit;

    @Column(nullable = false, length = 150)
    private String item;

    protected Ingredient() {
    }

    public Ingredient(Recipe recipe, int position, String quantity, String unit, String item) {
        this.recipe = recipe;
        this.position = position;
        this.quantity = quantity;
        this.unit = unit;
        this.item = item;
    }

    public Long getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public int getPosition() {
        return position;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public String getItem() {
        return item;
    }
}
