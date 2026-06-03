package com.example.template.repository;

import com.example.template.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    // Cari banyak tag sekaligus berdasarkan kumpulan ID
    Set<Tag> findByIdIn(Set<Long> ids);

    boolean existsByName(String name);
}
