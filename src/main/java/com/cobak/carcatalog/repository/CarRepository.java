package com.cobak.carcatalog.repository;

import com.cobak.carcatalog.model.entity.Car;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CarRepository extends JpaRepository<Car, UUID> {
}
