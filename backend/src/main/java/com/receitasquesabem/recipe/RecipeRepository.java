package com.receitasquesabem.recipe;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Query("select r from Recipe r join fetch r.author order by lower(r.name)")
    List<Recipe> findAllWithAuthor();

    @Query("select r from Recipe r join fetch r.author where r.author.id = :userId order by lower(r.name)")
    List<Recipe> findMineWithAuthor(Long userId);

    boolean existsByAuthorIdAndNameIgnoreCase(Long authorId, String name);

    boolean existsByAuthorIdAndNameIgnoreCaseAndIdNot(Long authorId, String name, Long id);
}
