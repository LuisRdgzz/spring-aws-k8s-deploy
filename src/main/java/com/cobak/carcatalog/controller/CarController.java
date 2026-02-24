package com.cobak.carcatalog.controller;

import com.cobak.carcatalog.model.dto.ApiResponse;
import com.cobak.carcatalog.model.dto.CarRequestDTO;
import com.cobak.carcatalog.model.dto.CarResponseDTO;
import com.cobak.carcatalog.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cars")
@RequiredArgsConstructor
@Tag(name = "Cars", description = "Car catalog management endpoints")
public class CarController {

    private final CarService carService;

    @GetMapping
    @Operation(summary = "List all cars with pagination")
    public ResponseEntity<ApiResponse<Page<CarResponseDTO>>> getAllCars(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        Page<CarResponseDTO> cars = carService.getAllCars(pageable);
        return ResponseEntity.ok(ApiResponse.success(cars));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a car by ID")
    public ResponseEntity<ApiResponse<CarResponseDTO>> getCarById(@PathVariable UUID id) {
        CarResponseDTO car = carService.getCarById(id);
        return ResponseEntity.ok(ApiResponse.success(car));
    }

    @PostMapping
    @Operation(summary = "Create a new car")
    public ResponseEntity<ApiResponse<CarResponseDTO>> createCar(
            @Valid @RequestBody CarRequestDTO request) {
        CarResponseDTO car = carService.createCar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, "Car created successfully", car));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing car")
    public ResponseEntity<ApiResponse<CarResponseDTO>> updateCar(
            @PathVariable UUID id,
            @Valid @RequestBody CarRequestDTO request) {
        CarResponseDTO car = carService.updateCar(id, request);
        return ResponseEntity.ok(ApiResponse.success(200, "Car updated successfully", car));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a car")
    public ResponseEntity<ApiResponse<Void>> deleteCar(@PathVariable UUID id) {
        carService.deleteCar(id);
        return ResponseEntity.ok(ApiResponse.success(200, "Car deleted successfully", null));
    }
}
