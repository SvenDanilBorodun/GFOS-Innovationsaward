package com.gfos.ideaboard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Benutzername ist erforderlich")
    @Size(min = 3, max = 50, message = "Benutzername muss zwischen 3 und 50 Zeichen lang sein")
    private String username;

    @NotBlank(message = "E-Mail ist erforderlich")
    @Email(message = "Ungültiges E-Mail-Format")
    private String email;

    @NotBlank(message = "Passwort ist erforderlich")
    @Size(min = 8, message = "Passwort muss mindestens 8 Zeichen lang sein")
    private String password;

    @NotBlank(message = "Vorname ist erforderlich")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Nachname ist erforderlich")
    @Size(max = 50)
    private String lastName;

    // Getters und Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
