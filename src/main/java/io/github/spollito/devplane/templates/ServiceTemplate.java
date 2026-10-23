package io.github.spollito.devplane.templates;

/**
 * A template that services can be created from.
 *
 * @param name the template's folder under {@code service-templates/}, which is also
 * how clients choose it
 * @param description what a project generated from the template is, in a few words
 */
public record ServiceTemplate(String name, String description) {

}
