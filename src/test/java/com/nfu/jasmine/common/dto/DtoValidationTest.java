package com.nfu.jasmine.common.dto;

import com.nfu.jasmine.cus.dto.AppointmentCreateDTO;
import com.nfu.jasmine.sys.dto.UserCreateDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void pageQueryShouldRestrictPageRange() {
        PageQueryDTO pageQueryDTO = new PageQueryDTO();
        pageQueryDTO.setPageNo(0L);
        pageQueryDTO.setPageSize(101L);

        Set<String> fields = validator.validate(pageQueryDTO)
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of("pageNo", "pageSize"), fields);
    }

    @Test
    void userCreateShouldRequireCoreFields() {
        UserCreateDTO userCreateDTO = new UserCreateDTO();

        Set<String> fields = validator.validate(userCreateDTO)
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(fields.contains("username"));
        assertTrue(fields.contains("password"));
        assertTrue(fields.contains("status"));
    }

    @Test
    void appointmentCreateShouldRequireDateAndContent() {
        AppointmentCreateDTO appointmentCreateDTO = new AppointmentCreateDTO();
        appointmentCreateDTO.setPhone("13677778888");

        Set<String> fields = validator.validate(appointmentCreateDTO)
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(Set.of("date", "content"), fields);
    }
}
