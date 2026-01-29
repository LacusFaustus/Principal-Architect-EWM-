package ru.practicum.compilations.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.compilations.model.Compilation;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    Page<Compilation> findAll(Pageable pageable);

    Page<Compilation> findByPinned(Boolean pinned, Pageable pageable);
}
