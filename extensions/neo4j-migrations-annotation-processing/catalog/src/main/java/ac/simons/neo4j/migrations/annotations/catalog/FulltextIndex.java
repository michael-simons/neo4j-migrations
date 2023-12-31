package ac.simons.neo4j.migrations.annotations.catalog;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
@Documented
public @interface FulltextIndex {
    /**
     * If this is not {@literal null} it has precedence over an implicit label (either no class annotations or one without
     * a dedicated label) but not over OGM or SDN6 annotations specifying the label or type explicitly.
     * Its use must be consistent throughout the class.
     *
     * @return The target label
     */
    String label() default "";

    /**
     * Use this if you want to define composite, fulltext index when using {@link FulltextIndex} on the class level.
     * Leave it empty when using on field level, otherwise an exception will be thrown.
     *
     * @return The list of properties to include in the composite.
     */
    String[] properties() default {};

    /**
     * Use this if you want to use a different fulltext analyzer for your index.
     * Be aware to active the option to allow further options on index creation.
     * @return name of the fulltext analyzer
     */
    String analyzer() default "";
}
