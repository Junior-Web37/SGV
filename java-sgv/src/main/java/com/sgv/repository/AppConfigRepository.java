package com.sgv.repository;

import com.sgv.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
}