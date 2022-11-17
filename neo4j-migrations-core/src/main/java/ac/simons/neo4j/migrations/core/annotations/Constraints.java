package ac.simons.neo4j.migrations.core.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Container for the repeable {@link Constraint} annotation.
 *
 * @soundtrack Juse Ju - Millennium
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Documented
public @interface Constraints {
	/**
	 * @return the actual value
	 */
	Constraint[] value();
}
