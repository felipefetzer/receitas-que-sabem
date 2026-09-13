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
@Table(name = "preparation_steps")
public class PreparationStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id")
    private PreparationSection section;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, columnDefinition = "text")
    private String instruction;

    /** Opcional — nem todos os passos têm tempo (R10). */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    protected PreparationStep() {
    }

    public PreparationStep(PreparationSection section, int position, String instruction, Integer durationMinutes) {
        this.section = section;
        this.position = position;
        this.instruction = instruction;
        this.durationMinutes = durationMinutes;
    }

    public Long getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public String getInstruction() {
        return instruction;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }
}
