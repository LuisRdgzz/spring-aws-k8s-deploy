package com.cobak.carcatalog.service.impl;

import com.cobak.carcatalog.exception.ResourceNotFoundException;
import com.cobak.carcatalog.model.dto.CarRequestDTO;
import com.cobak.carcatalog.model.dto.CarResponseDTO;
import com.cobak.carcatalog.model.entity.Car;
import com.cobak.carcatalog.repository.CarRepository;
import com.cobak.carcatalog.service.CarService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;

    @Override
    public Page<CarResponseDTO> getAllCars(Pageable pageable) {
        return carRepository.findAll(pageable).map(this::toResponseDTO);
    }

    @Override
    public CarResponseDTO getCarById(UUID id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + id));
        return toResponseDTO(car);
    }

    @Override
    @Transactional
    public CarResponseDTO createCar(CarRequestDTO request) {
        Car car = toEntity(request);
        Car saved = carRepository.save(car);
        return toResponseDTO(saved);
    }

    @Override
    @Transactional
    public CarResponseDTO updateCar(UUID id, CarRequestDTO request) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + id));

        car.setBrand(request.getBrand());
        car.setModel(request.getModel());
        car.setYear(request.getYear());
        car.setPrice(request.getPrice());
        car.setColor(request.getColor());
        car.setDescription(request.getDescription());
        car.setImageUrl(request.getImageUrl());

        Car updated = carRepository.save(car);
        return toResponseDTO(updated);
    }

    @Override
    @Transactional
    public void deleteCar(UUID id) {
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + id));
        carRepository.delete(car);
    }

    private CarResponseDTO toResponseDTO(Car car) {
        return CarResponseDTO.builder()
                .id(car.getId())
                .brand(car.getBrand())
                .model(car.getModel())
                .year(car.getYear())
                .price(car.getPrice())
                .color(car.getColor())
                .description(car.getDescription())
                .imageUrl(car.getImageUrl())
                .createdAt(car.getCreatedAt())
                .updatedAt(car.getUpdatedAt())
                .build();
    }

    private Car toEntity(CarRequestDTO request) {
        return Car.builder()
                .brand(request.getBrand())
                .model(request.getModel())
                .year(request.getYear())
                .price(request.getPrice())
                .color(request.getColor())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build();
    }
}
