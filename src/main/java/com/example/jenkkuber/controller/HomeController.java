package com.example.jenkkuber.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/hello") // Bu, endpoint'in adresi olacak (örn: http://localhost:8080/merhaba)
    public String sayHello() {
        return "hello world";
    }
}