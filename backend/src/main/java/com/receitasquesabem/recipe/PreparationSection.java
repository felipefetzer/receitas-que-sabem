package com.receitasquesabem.recipe;

import java.util.ArrayList;
import java.util.List;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "preparation_sections")
public class PreparationSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 120)
    private String title;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PreparationStep> steps = new ArrayList<>();

    protected PreparationSection() {
    }

    public PreparationSection(Recipe recipe, int position, String title) {
        this.recipe = recipe;
        this.position = position;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    public List<PreparationStep> getSteps() {
        return steps;
    }
}
