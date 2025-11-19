package com.example.kpkmod;

import java.time.LocalDate;

public class User {
    public String familiya;
    public String name;
    public String pozivnoy;
    public Gender gender;
    public LocalDate birthdate;
    public User(String familiya, String name, String pozivnoy, Gender gender, LocalDate birthdate) {
        this.familiya = familiya;
        this.name = name;
        this.pozivnoy = pozivnoy;
        this.gender = gender;
        this.birthdate = birthdate;
    }
}
