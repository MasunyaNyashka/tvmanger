package com.masunya.tariff.service;

import com.masunya.common.exception.BusinessException;
import com.masunya.tariff.dto.TariffCreateRequest;
import com.masunya.tariff.dto.TariffResponse;
import com.masunya.tariff.dto.TariffUpdateRequest;
import com.masunya.tariff.entity.Tariff;
import com.masunya.tariff.repository.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TariffService {
    private final TariffRepository tariffRepository;

    @Transactional(readOnly = true)
    public List<TariffResponse> getPublicList() {
        return tariffRepository.findAllByArchivedFalseOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TariffResponse getPublicById(UUID id) {
        Tariff tariff = tariffRepository.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new BusinessException("Tariff not found", HttpStatus.NOT_FOUND));
        return toResponse(tariff);
    }

    @Transactional(readOnly = true)
    public List<TariffResponse> getAllForAdmin() {
        return tariffRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TariffResponse create(TariffCreateRequest request) {
        if (tariffRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Tariff name already exists", HttpStatus.CONFLICT);
        }
        Tariff tariff = new Tariff();
        tariff.setId(UUID.randomUUID());
        tariff.setName(request.getName().trim());
        tariff.setPrice(request.getPrice());
        tariff.setConnectionType(request.getConnectionType());
        tariff.setDescription(request.getDescription());
        tariff.setChannels(request.getChannels());
        tariff.setArchived(false);
        tariff.setCreatedAt(Instant.now());
        return toResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public TariffResponse update(UUID id, TariffUpdateRequest request) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tariff not found", HttpStatus.NOT_FOUND));
        if (tariffRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new BusinessException("Tariff name already exists", HttpStatus.CONFLICT);
        }
        tariff.setName(request.getName().trim());
        tariff.setPrice(request.getPrice());
        tariff.setConnectionType(request.getConnectionType());
        tariff.setDescription(request.getDescription());
        tariff.setChannels(request.getChannels());
        return toResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public TariffResponse setArchived(UUID id, boolean archived) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tariff not found", HttpStatus.NOT_FOUND));
        tariff.setArchived(archived);
        return toResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public void delete(UUID id) {
        Tariff tariff = tariffRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Tariff not found", HttpStatus.NOT_FOUND));
        tariffRepository.delete(tariff);
    }

    private TariffResponse toResponse(Tariff tariff) {
        return TariffResponse.builder()
                .id(tariff.getId())
                .name(tariff.getName())
                .price(tariff.getPrice())
                .connectionType(tariff.getConnectionType())
                .description(tariff.getDescription())
                .channels(tariff.getChannels())
                .archived(tariff.isArchived())
                .build();
    }
}
