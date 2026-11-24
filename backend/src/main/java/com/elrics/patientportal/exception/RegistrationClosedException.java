package com.elrics.patientportal.exception;

public class RegistrationClosedException extends RuntimeException {
    public RegistrationClosedException() {
        super("Registration is closed: an administrator account already exists.");
    }
}
