package com.julianh06.wynnextras_server.repository;

import com.julianh06.wynnextras_server.entity.AspectPublicationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AspectPublicationPreferenceRepository
        extends JpaRepository<AspectPublicationPreference, String> {
}
