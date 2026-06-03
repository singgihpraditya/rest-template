package com.example.template.repository;

import com.example.template.entity.EndpointPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EndpointPermissionRepository extends JpaRepository<EndpointPermission, Long> {

    // Load semua rule aktif, diurutkan dari yang paling spesifik (sortOrder terkecil)
    List<EndpointPermission> findAllByActiveTrueOrderBySortOrderAsc();
}
