package org.zanata.action.validator;

import javax.validation.ConstraintValidatorContext;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zanata.dao.PersonDAO;
import org.zanata.model.HPerson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DuplicateEmailValidatorTest {
    private DuplicateEmailValidator validator;
    @Mock
    private PersonDAO personDAO;
    private ConstraintValidatorContext context = null;

    @Before
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
        validator = new DuplicateEmailValidator() {
            private static final long serialVersionUID = 1L;

            @Override
            protected PersonDAO getPersonDAO() {
                return personDAO;
            }
        };
        validator.initialize(null);
    }

    @Test
    public void nullOrEmptyStringIsValid() throws Exception {
        assertThat(validator.isValid(null, context)).isTrue();
        assertThat(validator.isValid("", context)).isTrue();
    }

    @Test
    public void notEmptyStringIsValidIfThereIsNoPersonWithThisEmail() {
        when(personDAO.findByEmail("a@b.c")).thenReturn(null);
        Assertions.assertThat(validator.isValid("a@b.c", context)).isTrue();
    }

    @Test
    public void notEmptyStringIsInvalidIfThereIsPersonWithThisEmail() {
        when(personDAO.findByEmail("a@b.c")).thenReturn(new HPerson());
        Assertions.assertThat(validator.isValid("a@b.c", context)).isFalse();
    }
}
