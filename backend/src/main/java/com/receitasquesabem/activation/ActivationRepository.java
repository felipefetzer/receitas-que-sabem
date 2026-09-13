package com.receitasquesabem.activation;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ActivationRepository extends JpaRepository<Activation, Long> {

    @Query("select a from Activation a join fetch a.recipe r join fetch r.author where a.user.id = :userId")
    List<Activation> findByUserId(Long userId);

    Optional<Activation> findByUserIdAndRecipeId(Long userId, Long recipeId);

    long countByUserId(Long userId);
}
