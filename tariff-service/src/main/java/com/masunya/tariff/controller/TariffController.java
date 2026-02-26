package com.masunya.tariff.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tariffs")
@RequiredArgsConstructor
public class TariffController {
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok().body("ok");
    }
}
