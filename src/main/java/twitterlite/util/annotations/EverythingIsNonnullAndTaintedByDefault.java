package twitterlite.util.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.annotation.Nonnull;
import javax.annotation.Tainted;
import javax.annotation.meta.TypeQualifierDefault;

@Documented
@Nonnull
@Tainted
@TypeQualifierDefault({ ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.LOCAL_VARIABLE })
@Retention(RetentionPolicy.RUNTIME)
public @interface EverythingIsNonnullAndTaintedByDefault {

}
