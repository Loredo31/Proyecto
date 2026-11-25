package com.proyecto.servicios.dto;

import com.proyecto.servicios.dto.cliente.DatosContactoDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El correo puede ser de cualquier dominio, pero debe llevar @ y terminar en .com o .mx. */
class CorreoValidacionTest {

    private static Validator validator;

    @BeforeAll
    static void iniciar() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static boolean correoValido(String correo) {
        DatosContactoDto dto = DatosContactoDto.builder().correoElectronico(correo).telefonoMovil(4181234567L).build();
        return validator.validateProperty(dto, "correoElectronico").isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"juan@hotmail.com", "ana.lopez@gmail.com", "alumno@utng.edu.mx", "x_y+1@empresa.com.mx", "JUAN@HOTMAIL.COM"})
    void aceptaCorreosQueTerminanEnComOMx(String correo) {
        assertTrue(correoValido(correo), correo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"juan@hotmail.comm", "juan@hotmail.net", "juan@hotmail", "juanhotmail.com", "juan@.com", "juan @hotmail.com", "juan@hotmail.co"})
    void rechazaOtrasTerminacionesOSinArroba(String correo) {
        assertFalse(correoValido(correo), correo);
    }
}
