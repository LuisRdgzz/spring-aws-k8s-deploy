package com.cobak.carcatalog.service;

import com.cobak.carcatalog.model.dto.CarRequestDTO;
import com.cobak.carcatalog.model.dto.CarResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CarService {

    Page<CarResponseDTO> getAllCars(Pageable pageable);

    CarResponseDTO getCarById(UUID id);

    CarResponseDTO createCar(CarRequestDTO request);

    CarResponseDTO updateCar(UUID id, CarRequestDTO request);

    void deleteCar(UUID id);
}
