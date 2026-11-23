package be.appify.prefab.core.diagnostics;

import io.awspring.cloud.sns.core.SnsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import static org.assertj.core.api.Assertions.assertThat;

class FailureAnalyzersTest {

    @Test
    void keepsTheOriginalExceptionAsCauseOfTheAnalysis() {
        var missingBean = new NoSuchBeanDefinitionException(SnsTemplate.class);
        var failure = new BeanCreationException("Error creating bean", missingBean);

        var analysis = new MessagingBeanFailureAnalyzer().analyze(failure);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getCause()).isSameAs(failure);
    }

    @Test
    void leavesUnknownMissingBeansToSpringBoot() {
        var failure = new NoSuchBeanDefinitionException(String.class);

        assertThat(new MessagingBeanFailureAnalyzer().analyze(failure)).isNull();
    }

    @Test
    void reportsDescriptionAndActionOfAConfigurationException() {
        var failure = new PrefabConfigurationException("Something is wrong.", "Fix it.");

        var analysis = new PrefabConfigurationFailureAnalyzer().analyze(failure);

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription()).isEqualTo("Something is wrong.");
        assertThat(analysis.getAction()).isEqualTo("Fix it.");
        assertThat(analysis.getCause()).isSameAs(failure);
    }
}
