package cz.cas.mbu.cygenexpi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cz.cas.mbu.cygenexpi.RememberValue.Type;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RememberAllValues {
	RememberValue.Type type() default RememberValue.Type.PERMANENTLY;	
	boolean restrictToTunables() default false;
}
