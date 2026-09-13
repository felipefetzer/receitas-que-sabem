package com.receitasquesabem.activation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.receitasquesabem.recipe.Recipe;
import com.receitasquesabem.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Uma receita que o utilizador está a fazer.
 * Apagar esta linha faz desaparecer, em cascata, o estado dos
 * ingredientes — e com ele os itens da lista de compras (R15).
 */
@Entity
@Table(name = "activations")
public class Activation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(name = "activated_at", insertable = false, updatable = false)
    private Instant activatedAt;

    @OneToMany(mappedBy = "activation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivationIngredient> ingredients = new ArrayList<>();

    protected Activation() {
    }

    public Activation(User user, Recipe recipe) {
        this.user = user;
        this.recipe = recipe;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public List<ActivationIngredient> getIngredients() {
        return ingredients;
    }
}
