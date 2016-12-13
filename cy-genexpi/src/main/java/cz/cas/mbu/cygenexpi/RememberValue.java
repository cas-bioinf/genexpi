package cz.cas.mbu.cygenexpi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RememberValue {
	public enum Type {NEVER, SESSION, PERMANENTLY};
	
	Type type() default Type.PERMANENTLY;	
}
